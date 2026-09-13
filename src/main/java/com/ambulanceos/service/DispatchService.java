package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.exception.AmbulanceNotFoundException;
import com.ambulanceos.exception.DispatchNotFoundException;
import com.ambulanceos.exception.EmergencyNotFoundException;
import com.ambulanceos.exception.NoAvailableAmbulanceException;
import com.ambulanceos.exception.RouteNotFoundException;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.repository.EmergencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final AmbulanceRepository ambulanceRepository;

    private final EmergencyRepository emergencyRepository;

    private final DispatchRepository dispatchRepository;

    private final GurgaonRoadGraph roadGraph;

    /*
     * Central routing gateway.
     *
     * The routing algorithm can be:
     *
     *     DIJKSTRA
     *     ASTAR
     *
     * DispatchService does not directly depend on a specific
     * routing algorithm implementation.
     */
    private final RoutingService routingService;

    /*
     * Provides the currently configured routing algorithm.
     *
     * This is used by the public one-argument method.
     *
     * DispatchPlanService can explicitly provide an algorithm
     * using the overloaded two-argument method.
     */
    private final RoutingSettingsService routingSettingsService;


    // =========================================================
    // FIND BEST AMBULANCE
    // =========================================================
    //
    // Public convenience method used by the existing
    // DispatchController.
    //
    // It reads the currently configured routing algorithm and
    // delegates to the algorithm-aware method.
    //
    // =========================================================

    @Transactional
    public DispatchResponse findBestAmbulance(
            Long emergencyId
    ) {

        RoutingAlgorithm selectedAlgorithm =
                routingSettingsService.getRoutingAlgorithm();

        return findBestAmbulance(
                emergencyId,
                selectedAlgorithm
        );
    }


    // =========================================================
    // FIND BEST AMBULANCE WITH EXPLICIT ALGORITHM
    // =========================================================
    //
    // Finds the fastest reachable AVAILABLE ambulance.
    //
    // The supplied routing algorithm is used for every
    // ambulance candidate.
    //
    // This method is used by DispatchPlanService so that
    // the entire dispatch plan uses the same algorithm.
    //
    // AVAILABLE ambulance rows are locked using a
    // database-level PESSIMISTIC_WRITE lock.
    //
    // =========================================================

    @Transactional
    public DispatchResponse findBestAmbulance(
            Long emergencyId,
            RoutingAlgorithm selectedAlgorithm
    ) {

        // -----------------------------------------------------
        // 1. VALIDATE ROUTING ALGORITHM
        // -----------------------------------------------------

        if (selectedAlgorithm == null) {

            throw new IllegalArgumentException(
                    "Routing algorithm must not be null"
            );
        }


        // -----------------------------------------------------
        // 2. FIND EMERGENCY
        // -----------------------------------------------------

        Emergency emergency =
                emergencyRepository.findById(
                        emergencyId
                ).orElseThrow(() ->
                        new EmergencyNotFoundException(
                                emergencyId
                        )
                );


        // -----------------------------------------------------
        // 3. FIND NEAREST GRAPH NODE FOR EMERGENCY
        // -----------------------------------------------------

        GraphNode emergencyNode =
                roadGraph.findNearestNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                );


        if (emergencyNode == null) {

            throw new NoAvailableAmbulanceException(
                    "Unable to find road node near emergency"
            );
        }


        // -----------------------------------------------------
        // 4. GET AVAILABLE AMBULANCES WITH DATABASE LOCK
        // -----------------------------------------------------

        List<Ambulance> availableAmbulances =
                ambulanceRepository.findByStatusIgnoreCase(
                        "AVAILABLE"
                );


        if (availableAmbulances.isEmpty()) {

            throw new NoAvailableAmbulanceException(
                    "No available ambulance found"
            );
        }


        // =====================================================
        // 5. PRIORITY QUEUE
        // =====================================================
        //
        // Lower traffic-adjusted travel time means higher
        // priority.
        //
        // =====================================================

        PriorityQueue<AmbulanceDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                AmbulanceDistance::travelTimeMinutes
                        )
                );


        // =====================================================
        // 6. CALCULATE ROUTE FOR EACH AMBULANCE
        // =====================================================

        for (Ambulance ambulance :
                availableAmbulances) {

            GraphNode ambulanceNode =
                    roadGraph.findNearestNode(
                            ambulance.getLatitude(),
                            ambulance.getLongitude()
                    );


            if (ambulanceNode == null) {

                log.warn(
                        "Unable to find road node for ambulance {}",
                        ambulance.getAmbulanceNumber()
                );

                continue;
            }


            try {

                /*
                 * Use the central routing gateway.
                 *
                 * The explicitly supplied algorithm is used
                 * for this candidate.
                 */
                TrafficDijkstraResponse route =
                        routingService.findRoute(
                                ambulanceNode.id(),
                                emergencyNode.id(),
                                selectedAlgorithm
                        );


                priorityQueue.offer(
                        new AmbulanceDistance(
                                ambulance,
                                route.distanceKm(),
                                route.estimatedTravelTimeMinutes(),
                                route.trafficLevel()
                        )
                );

            } catch (RouteNotFoundException exception) {

                // -------------------------------------------------
                // The ambulance exists but is unreachable through
                // the current road graph.
                //
                // Skip it and evaluate the remaining ambulances.
                // -------------------------------------------------

                log.warn(
                        "No route found for ambulance {}: {}",
                        ambulance.getAmbulanceNumber(),
                        exception.getMessage()
                );
            }
        }


        // =====================================================
        // 7. CHECK WHETHER ANY AMBULANCE IS REACHABLE
        // =====================================================

        if (priorityQueue.isEmpty()) {

            throw new NoAvailableAmbulanceException(
                    "No reachable available ambulance found"
            );
        }


        // =====================================================
        // 8. GET FASTEST AMBULANCE
        // =====================================================

        AmbulanceDistance best =
                priorityQueue.poll();


        Ambulance ambulance =
                best.ambulance();


        // =====================================================
        // 9. RETURN RESULT
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
    //     AVAILABLE
    //          ↓
    //     EN_ROUTE
    //
    // This operation is transactional.
    //
    // =========================================================

    @Transactional
    public DispatchResponse dispatchAmbulance(
            Long emergencyId
    ) {

        // -----------------------------------------------------
        // Find best ambulance.
        //
        // The database lock acquired during ambulance selection
        // participates in this transaction.
        // -----------------------------------------------------

        DispatchResponse bestAmbulance =
                findBestAmbulance(
                        emergencyId
                );


        // -----------------------------------------------------
        // Load selected ambulance
        // -----------------------------------------------------

        Ambulance ambulance =
                ambulanceRepository.findById(
                        bestAmbulance.ambulanceId()
                ).orElseThrow(() ->
                        new AmbulanceNotFoundException(
                                bestAmbulance.ambulanceId()
                        )
                );


        // -----------------------------------------------------
        // Defensive status check
        // -----------------------------------------------------

        if (!"AVAILABLE".equalsIgnoreCase(
                ambulance.getStatus()
        )) {

            throw new NoAvailableAmbulanceException(
                    "Selected ambulance is no longer available"
            );
        }


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
    // IN_PROGRESS
    //       ↓
    // COMPLETED
    //
    // Ambulance:
    //
    // EN_ROUTE / active dispatch
    //       ↓
    // AVAILABLE
    //
    // Both changes happen inside one transaction.
    //
    // =========================================================

    @Transactional
    public Dispatch completeDispatch(
            Long dispatchId
    ) {

        // -----------------------------------------------------
        // 1. FIND DISPATCH
        // -----------------------------------------------------

        Dispatch dispatch =
                dispatchRepository.findById(
                        dispatchId
                ).orElseThrow(() ->
                        new DispatchNotFoundException(
                                dispatchId
                        )
                );


        // -----------------------------------------------------
        // 2. PREVENT DUPLICATE COMPLETION
        // -----------------------------------------------------

        if ("COMPLETED".equalsIgnoreCase(
                dispatch.getStatus()
        )) {

            return dispatch;
        }


        // -----------------------------------------------------
        // 3. FIND ASSIGNED AMBULANCE
        // -----------------------------------------------------

        Ambulance ambulance =
                ambulanceRepository.findById(
                        dispatch.getAmbulanceId()
                ).orElseThrow(() ->
                        new AmbulanceNotFoundException(
                                dispatch.getAmbulanceId()
                        )
                );


        // -----------------------------------------------------
        // 4. RETURN AMBULANCE TO AVAILABLE STATE
        //
        // Active dispatch → AVAILABLE
        // -----------------------------------------------------

        ambulance.setStatus(
                "AVAILABLE"
        );


        ambulanceRepository.save(
                ambulance
        );


        // -----------------------------------------------------
        // 5. COMPLETE DISPATCH
        // -----------------------------------------------------

        dispatch.setStatus(
                "COMPLETED"
        );


        // -----------------------------------------------------
        // 6. STORE COMPLETION TIMESTAMP
        // -----------------------------------------------------

        dispatch.setCompletedAt(
                LocalDateTime.now()
        );


        // -----------------------------------------------------
        // 7. SAVE COMPLETED DISPATCH
        // -----------------------------------------------------

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