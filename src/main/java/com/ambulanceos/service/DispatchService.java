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
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.repository.EmergencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final TrafficAwareDijkstraService
            trafficAwareDijkstraService;


    // =========================================================
    // FIND BEST AMBULANCE
    // =========================================================
    //
    // Finds the fastest reachable AVAILABLE ambulance.
    //
    // The AVAILABLE ambulance rows are locked using a
    // database-level PESSIMISTIC_WRITE lock.
    //
    // This is important because ambulance selection and status
    // update must not race with another dispatch request.
    //
    // =========================================================

    @Transactional
    public DispatchResponse findBestAmbulance(
            Long emergencyId
    ) {

        // -----------------------------------------------------
        // 1. FIND EMERGENCY
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
        // 2. FIND NEAREST GRAPH NODE FOR EMERGENCY
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
        // 3. GET AVAILABLE AMBULANCES WITH DATABASE LOCK
        // -----------------------------------------------------
        //
        // IMPORTANT:
        //
        // AmbulanceRepository uses:
        //
        // @Lock(PESSIMISTIC_WRITE)
        //
        // Therefore PostgreSQL locks these rows until the
        // surrounding transaction completes.
        //
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


        // -----------------------------------------------------
        // 4. PRIORITY QUEUE
        // -----------------------------------------------------
        //
        // Lower travel time = higher priority.
        //
        // -----------------------------------------------------

        PriorityQueue<AmbulanceDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                AmbulanceDistance::travelTimeMinutes
                        )
                );


        // -----------------------------------------------------
        // 5. CALCULATE ROUTE FOR EACH AMBULANCE
        // -----------------------------------------------------

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

                TrafficDijkstraResponse route =
                        trafficAwareDijkstraService
                                .findShortestPath(
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
                // Ambulance exists but may be unreachable in the
                // road graph.
                //
                // Skip it and continue checking other ambulances.
                // -------------------------------------------------

                System.out.println(
                        "No route found for ambulance "
                                + ambulance.getAmbulanceNumber()
                                + ": "
                                + exception.getMessage()
                );
            }
        }


        // -----------------------------------------------------
        // 6. CHECK WHETHER ANY AMBULANCE IS REACHABLE
        // -----------------------------------------------------

        if (priorityQueue.isEmpty()) {

            throw new NoAvailableAmbulanceException(
                    "No reachable available ambulance found"
            );
        }


        // -----------------------------------------------------
        // 7. GET FASTEST AMBULANCE
        // -----------------------------------------------------

        AmbulanceDistance best =
                priorityQueue.poll();

        Ambulance ambulance =
                best.ambulance();


        // -----------------------------------------------------
        // 8. RETURN RESULT
        // -----------------------------------------------------

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
    // The whole operation runs inside one transaction.
    //
    // Therefore the database lock acquired during ambulance
    // selection remains active until the status update commits.
    //
    // =========================================================

    @Transactional
    public DispatchResponse dispatchAmbulance(
            Long emergencyId
    ) {

        // -----------------------------------------------------
        // Find best ambulance.
        //
        // The database lock acquired by findBestAmbulance()
        // participates in the same transaction.
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
                        new AmbulanceNotFoundException(
                                bestAmbulance.ambulanceId()
                        )
                );


        // -----------------------------------------------------
        // Defensive status check
        // -----------------------------------------------------
        //
        // Normally this should always be AVAILABLE because
        // findBestAmbulance() selected it while holding the
        // database lock.
        //
        // This check provides an additional safety layer.
        //
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
// At the same time:
//
// Ambulance EN_ROUTE
//       ↓
// AVAILABLE
//
// Both changes happen inside one transaction so the database
// does not end up with a completed dispatch whose ambulance
// is still marked as busy.
//
// =========================================================

    @Transactional
    public Dispatch completeDispatch(
            Long dispatchId
    ) {

        // ---------------------------------------------------------
        // 1. FIND DISPATCH
        // ---------------------------------------------------------

        Dispatch dispatch =
                dispatchRepository.findById(
                        dispatchId
                ).orElseThrow(() ->
                        new DispatchNotFoundException(
                                dispatchId
                        )
                );


        // ---------------------------------------------------------
        // 2. PREVENT DUPLICATE COMPLETION
        // ---------------------------------------------------------

        if ("COMPLETED".equalsIgnoreCase(
                dispatch.getStatus()
        )) {

            return dispatch;
        }


        // ---------------------------------------------------------
        // 3. FIND ASSIGNED AMBULANCE
        // ---------------------------------------------------------

        Ambulance ambulance =
                ambulanceRepository.findById(
                        dispatch.getAmbulanceId()
                ).orElseThrow(() ->
                        new AmbulanceNotFoundException(
                                dispatch.getAmbulanceId()
                        )
                );


        // ---------------------------------------------------------
        // 4. RETURN AMBULANCE TO AVAILABLE STATE
        // ---------------------------------------------------------
        //
        // EN_ROUTE → AVAILABLE
        //
        // The ambulance is now eligible for another emergency.
        //
        // ---------------------------------------------------------

        ambulance.setStatus(
                "AVAILABLE"
        );

        ambulanceRepository.save(
                ambulance
        );


        // ---------------------------------------------------------
        // 5. COMPLETE DISPATCH
        // ---------------------------------------------------------

        dispatch.setStatus(
                "COMPLETED"
        );


        // ---------------------------------------------------------
        // 6. STORE COMPLETION TIMESTAMP
        // ---------------------------------------------------------

        dispatch.setCompletedAt(
                LocalDateTime.now()
        );


        // ---------------------------------------------------------
        // 7. SAVE COMPLETED DISPATCH
        // ---------------------------------------------------------

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