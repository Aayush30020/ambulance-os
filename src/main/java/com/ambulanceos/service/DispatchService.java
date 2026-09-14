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

    private final RoutingService routingService;

    private final RoutingSettingsService routingSettingsService;

    // =========================================================
    // FIND BEST AMBULANCE
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

    @Transactional
    public DispatchResponse findBestAmbulance(
            Long emergencyId,
            RoutingAlgorithm selectedAlgorithm
    ) {

        if (selectedAlgorithm == null) {

            throw new IllegalArgumentException(
                    "Routing algorithm must not be null"
            );
        }

        Emergency emergency =
                emergencyRepository.findById(
                        emergencyId
                ).orElseThrow(() ->
                        new EmergencyNotFoundException(
                                emergencyId
                        )
                );

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

        List<Ambulance> availableAmbulances =
                ambulanceRepository.findByStatusIgnoreCase(
                        "AVAILABLE"
                );

        if (availableAmbulances.isEmpty()) {

            throw new NoAvailableAmbulanceException(
                    "No available ambulance found"
            );
        }

        PriorityQueue<AmbulanceDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                AmbulanceDistance::travelTimeMinutes
                        )
                );

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

                log.warn(
                        "No route found for ambulance {}: {}",
                        ambulance.getAmbulanceNumber(),
                        exception.getMessage()
                );
            }
        }

        if (priorityQueue.isEmpty()) {

            throw new NoAvailableAmbulanceException(
                    "No reachable available ambulance found"
            );
        }

        AmbulanceDistance best =
                priorityQueue.poll();

        Ambulance ambulance =
                best.ambulance();

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

    @Transactional
    public DispatchResponse dispatchAmbulance(
            Long emergencyId
    ) {

        DispatchResponse bestAmbulance =
                findBestAmbulance(
                        emergencyId
                );

        Ambulance ambulance =
                ambulanceRepository.findById(
                        bestAmbulance.ambulanceId()
                ).orElseThrow(() ->
                        new AmbulanceNotFoundException(
                                bestAmbulance.ambulanceId()
                        )
                );

        if (!"AVAILABLE".equalsIgnoreCase(
                ambulance.getStatus()
        )) {

            throw new NoAvailableAmbulanceException(
                    "Selected ambulance is no longer available"
            );
        }

        ambulance.setStatus(
                "EN_ROUTE"
        );

        Ambulance updatedAmbulance =
                ambulanceRepository.save(
                        ambulance
                );

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
    // This is now the authoritative completion operation.
    //
    // IN_PROGRESS
    //       ↓
    // COMPLETED
    //
    // Emergency:
    //
    // RESPONDING
    //       ↓
    // COMPLETED
    //
    // Ambulance:
    //
    // EN_ROUTE
    //       ↓
    // AVAILABLE
    //
    // ALL THREE CHANGES HAPPEN IN ONE TRANSACTION.
    //
    // This prevents the frontend from leaving the database
    // halfway through the completion lifecycle.
    //
    // =========================================================

    @Transactional
    public Dispatch completeDispatch(
            Long dispatchId
    ) {

        Dispatch dispatch =
                dispatchRepository.findById(
                        dispatchId
                ).orElseThrow(() ->
                        new DispatchNotFoundException(
                                dispatchId
                        )
                );

        // -----------------------------------------------------
        // Find associated emergency
        // -----------------------------------------------------

        Emergency emergency =
                emergencyRepository.findById(
                        dispatch.getEmergencyId()
                ).orElseThrow(() ->
                        new EmergencyNotFoundException(
                                dispatch.getEmergencyId()
                        )
                );

        // -----------------------------------------------------
        // Idempotent completion
        // -----------------------------------------------------
        //
        // If the dispatch is already completed, make sure the
        // emergency is also completed and return safely.
        //
        // -----------------------------------------------------

        if ("COMPLETED".equalsIgnoreCase(
                dispatch.getStatus()
        )) {

            if (!"COMPLETED".equalsIgnoreCase(
                    emergency.getStatus()
            )) {

                emergency.setStatus(
                        "COMPLETED"
                );

                emergencyRepository.save(
                        emergency
                );
            }

            return dispatch;
        }

        // -----------------------------------------------------
        // Find assigned ambulance
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
        // Return ambulance to AVAILABLE
        // -----------------------------------------------------

        ambulance.setStatus(
                "AVAILABLE"
        );

        ambulanceRepository.save(
                ambulance
        );

        // -----------------------------------------------------
        // Complete dispatch
        // -----------------------------------------------------

        dispatch.setStatus(
                "COMPLETED"
        );

        dispatch.setCompletedAt(
                LocalDateTime.now()
        );

        Dispatch completedDispatch =
                dispatchRepository.save(
                        dispatch
                );

        // -----------------------------------------------------
        // Complete associated emergency
        // -----------------------------------------------------
        //
        // This is the key permanent consistency fix.
        //
        // Dispatch completion and emergency completion are now
        // part of the same database transaction.
        //
        // -----------------------------------------------------

        if (!"COMPLETED".equalsIgnoreCase(
                emergency.getStatus()
        )) {

            emergency.setStatus(
                    "COMPLETED"
            );

            emergencyRepository.save(
                    emergency
            );
        }

        return completedDispatch;
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