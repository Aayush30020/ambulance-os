package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.repository.EmergencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final AmbulanceRepository ambulanceRepository;

    private final EmergencyRepository emergencyRepository;

    private final DispatchRepository dispatchRepository;

    private final GurgaonRoadGraph roadGraph;

    /*
     * Central routing gateway.
     *
     * RoutingService automatically uses the algorithm selected
     * in the system settings:
     *
     * DIJKSTRA
     * or
     * ASTAR
     */
    private final RoutingService routingService;


    // =========================================================
    // FIND BEST AMBULANCE
    // =========================================================
    //
    // Finds the fastest reachable AVAILABLE ambulance.
    //
    // Process:
    //
    // 1. Find emergency
    // 2. Find nearest graph node
    // 3. Get AVAILABLE ambulances
    // 4. Calculate route for every ambulance
    // 5. Store candidates in PriorityQueue
    // 6. Select ambulance with minimum travel time
    //
    // RoutingService decides whether Dijkstra or A* is used.
    //
    // =========================================================

    public DispatchResponse findBestAmbulance(
            Long emergencyId
    ) {

        // =====================================================
        // 1. FIND EMERGENCY
        // =====================================================

        Emergency emergency =
                emergencyRepository.findById(
                        emergencyId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Emergency not found with id: "
                                        + emergencyId
                        )
                );


        // =====================================================
        // 2. FIND NEAREST GRAPH NODE FOR EMERGENCY
        // =====================================================

        GraphNode emergencyNode =
                roadGraph.findNearestNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                );


        if (emergencyNode == null) {

            throw new RuntimeException(
                    "Unable to find road node near emergency"
            );
        }


        // =====================================================
        // 3. GET AVAILABLE AMBULANCES
        // =====================================================

        List<Ambulance> availableAmbulances =
                ambulanceRepository.findAll()
                        .stream()
                        .filter(ambulance ->
                                "AVAILABLE".equalsIgnoreCase(
                                        ambulance.getStatus()
                                )
                        )
                        .toList();


        if (availableAmbulances.isEmpty()) {

            throw new RuntimeException(
                    "No available ambulance found"
            );
        }


        // =====================================================
        // 4. PRIORITY QUEUE
        // =====================================================
        //
        // Lower travel time = higher priority.
        //
        // PriorityQueue is the main DSA component used
        // for ambulance selection.
        //
        // =====================================================

        PriorityQueue<AmbulanceDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                AmbulanceDistance::travelTimeMinutes
                        )
                );


        // =====================================================
        // 5. CALCULATE ROUTE FOR EACH AMBULANCE
        // =====================================================

        for (Ambulance ambulance :
                availableAmbulances) {

            GraphNode ambulanceNode =
                    roadGraph.findNearestNode(
                            ambulance.getLatitude(),
                            ambulance.getLongitude()
                    );


            if (ambulanceNode == null) {

                System.out.println(
                        "Unable to find road node for ambulance "
                                + ambulance.getAmbulanceNumber()
                );

                continue;
            }


            try {

                // -------------------------------------------------
                // RoutingService automatically selects:
                //
                // Traffic-aware Dijkstra
                //
                // OR
                //
                // A*
                // -------------------------------------------------

                TrafficDijkstraResponse route =
                        routingService.findRoute(
                                ambulanceNode.id(),
                                emergencyNode.id()
                        );


                priorityQueue.offer(
                        new AmbulanceDistance(
                                ambulance,
                                route.distanceKm(),
                                route.estimatedTravelTimeMinutes(),
                                route.trafficLevel()
                        )
                );


            } catch (RuntimeException exception) {

                // -------------------------------------------------
                // Ambulance may be present in the database but
                // unreachable in the road graph.
                // -------------------------------------------------

                System.out.println(
                        "No route found for ambulance "
                                + ambulance.getAmbulanceNumber()
                                + ": "
                                + exception.getMessage()
                );
            }
        }


        // =====================================================
        // 6. CHECK WHETHER ANY AMBULANCE IS REACHABLE
        // =====================================================

        if (priorityQueue.isEmpty()) {

            throw new RuntimeException(
                    "No reachable available ambulance found"
            );
        }


        // =====================================================
        // 7. GET FASTEST AMBULANCE
        // =====================================================

        AmbulanceDistance best =
                priorityQueue.poll();

        Ambulance ambulance =
                best.ambulance();


        // =====================================================
        // 8. RETURN RESULT
        // =====================================================

        return new DispatchResponse(

                emergency.getId(),

                ambulance.getId(),

                ambulance.getAmbulanceNumber(),

                ambulance.getType(),

                ambulance.getStatus(),

                roundToTwoDecimals(
                        best.distanceKm()
                ),

                roundToTwoDecimals(
                        best.travelTimeMinutes()
                ),

                best.trafficLevel()
        );
    }


    // =========================================================
    // DISPATCH AMBULANCE
    // =========================================================
    //
    // Finds the best ambulance and changes:
    //
    // AVAILABLE
    //      ↓
    // EN_ROUTE
    //
    // Persistent Dispatch history is created by
    // DispatchPlanService.
    //
    // =========================================================

    public DispatchResponse dispatchAmbulance(
            Long emergencyId
    ) {

        // -----------------------------------------------------
        // Find best ambulance
        // -----------------------------------------------------

        DispatchResponse bestAmbulance =
                findBestAmbulance(
                        emergencyId
                );


        // -----------------------------------------------------
        // Load ambulance from database
        // -----------------------------------------------------

        Ambulance ambulance =
                ambulanceRepository.findById(
                        bestAmbulance.ambulanceId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Selected ambulance not found"
                        )
                );


        // -----------------------------------------------------
        // Change status
        //
        // AVAILABLE → EN_ROUTE
        // -----------------------------------------------------

        ambulance.setStatus(
                "EN_ROUTE"
        );


        // -----------------------------------------------------
        // Save updated ambulance
        // -----------------------------------------------------

        Ambulance updatedAmbulance =
                ambulanceRepository.save(
                        ambulance
                );


        // -----------------------------------------------------
        // Return updated response
        // -----------------------------------------------------

        return new DispatchResponse(

                emergencyId,

                updatedAmbulance.getId(),

                updatedAmbulance.getAmbulanceNumber(),

                updatedAmbulance.getType(),

                updatedAmbulance.getStatus(),

                bestAmbulance.distanceKm(),

                bestAmbulance.estimatedTravelTimeMinutes(),

                bestAmbulance.trafficLevel()
        );
    }


    // =========================================================
    // COMPLETE DISPATCH
    // =========================================================
    //
    // Dispatch lifecycle:
    //
    // IN_PROGRESS
    //       ↓
    // COMPLETED
    //
    // completedAt is also stored.
    //
    // Ambulance lifecycle:
    //
    // EN_ROUTE
    //       ↓
    // AVAILABLE
    //
    // Once the dispatch is completed, the ambulance is
    // automatically released and can handle another emergency.
    //
    // =========================================================

    public Dispatch completeDispatch(
            Long dispatchId
    ) {

        // =====================================================
        // 1. FIND DISPATCH
        // =====================================================

        Dispatch dispatch =
                dispatchRepository.findById(
                        dispatchId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Dispatch not found with id: "
                                        + dispatchId
                        )
                );


        // =====================================================
        // 2. PREVENT DUPLICATE COMPLETION
        // =====================================================

        if ("COMPLETED".equalsIgnoreCase(
                dispatch.getStatus()
        )) {

            return dispatch;
        }


        // =====================================================
        // 3. UPDATE DISPATCH STATUS
        // =====================================================

        dispatch.setStatus(
                "COMPLETED"
        );


        // =====================================================
        // 4. STORE COMPLETION TIMESTAMP
        // =====================================================

        dispatch.setCompletedAt(
                LocalDateTime.now()
        );


        // =====================================================
        // 5. FIND AMBULANCE USED BY THIS DISPATCH
        // =====================================================

        Ambulance ambulance =
                ambulanceRepository.findById(
                        dispatch.getAmbulanceId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Ambulance not found for dispatch: "
                                        + dispatchId
                        )
                );


        // =====================================================
        // 6. RELEASE AMBULANCE
        //
        // EN_ROUTE → AVAILABLE
        // =====================================================

        ambulance.setStatus(
                "AVAILABLE"
        );


        // =====================================================
        // 7. SAVE UPDATED AMBULANCE
        // =====================================================

        ambulanceRepository.save(
                ambulance
        );


        // =====================================================
        // 8. SAVE COMPLETED DISPATCH
        // =====================================================

        return dispatchRepository.save(
                dispatch
        );
    }


    // =========================================================
    // ROUND NUMBER
    // =========================================================

    private double roundToTwoDecimals(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    // =========================================================
    // PRIORITY QUEUE ELEMENT
    // =========================================================

    private record AmbulanceDistance(

            Ambulance ambulance,

            double distanceKm,

            double travelTimeMinutes,

            String trafficLevel

    ) {
    }
}