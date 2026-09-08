package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.EmergencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final AmbulanceRepository ambulanceRepository;

    private final EmergencyRepository emergencyRepository;

    private final GurgaonRoadGraph roadGraph;

    private final TrafficAwareDijkstraService
            trafficAwareDijkstraService;


    // =========================================================
    // FIND BEST AVAILABLE AMBULANCE
    // =========================================================
    //
    // Emergency
    //      ↓
    // AVAILABLE ambulances
    //      ↓
    // Nearest OSM nodes
    //      ↓
    // Traffic-aware Dijkstra
    //      ↓
    // Estimated travel time
    //      ↓
    // PriorityQueue
    //      ↓
    // Fastest ambulance
    //
    // =========================================================

    public DispatchResponse findBestAmbulance(
            Long emergencyId
    ) {

        // =====================================================
        // STEP 1: FIND EMERGENCY
        // =====================================================

        Emergency emergency =
                emergencyRepository.findById(
                                emergencyId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency not found with id: "
                                                + emergencyId
                                )
                        );


        // =====================================================
        // STEP 2: FIND NEAREST GRAPH NODE
        // =====================================================

        GraphNode emergencyNode =
                roadGraph.findNearestNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                );


        // =====================================================
        // STEP 3: GET ALL AMBULANCES
        // =====================================================

        List<Ambulance> ambulances =
                ambulanceRepository.findAll();


        // =====================================================
        // STEP 4: PRIORITY QUEUE
        // =====================================================
        //
        // Ambulance with the lowest traffic-adjusted
        // travel time gets highest priority.
        //
        // =====================================================

        PriorityQueue<AmbulanceDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                AmbulanceDistance::travelTimeMinutes
                        )
                );


        // =====================================================
        // STEP 5: EVALUATE AVAILABLE AMBULANCES
        // =====================================================

        for (Ambulance ambulance : ambulances) {

            if (!"AVAILABLE".equalsIgnoreCase(
                    ambulance.getStatus()
            )) {

                continue;
            }


            // -------------------------------------------------
            // Map ambulance GPS location to nearest OSM node.
            // -------------------------------------------------

            GraphNode ambulanceNode =
                    roadGraph.findNearestNode(
                            ambulance.getLatitude(),
                            ambulance.getLongitude()
                    );


            try {

                // -------------------------------------------------
                // Traffic-aware Dijkstra
                //
                // Ambulance → Emergency
                // -------------------------------------------------

                TrafficDijkstraResponse route =
                        trafficAwareDijkstraService
                                .findShortestPath(
                                        ambulanceNode.id(),
                                        emergencyNode.id()
                                );


                // -------------------------------------------------
                // Add ambulance candidate to PriorityQueue.
                // -------------------------------------------------

                priorityQueue.offer(
                        new AmbulanceDistance(
                                ambulance,
                                route.distanceKm(),
                                route.estimatedTravelTimeMinutes(),
                                route.trafficLevel()
                        )
                );

            } catch (RuntimeException exception) {

                System.out.println(
                        "No traffic-aware route found for ambulance "
                                + ambulance.getAmbulanceNumber()
                                + ": "
                                + exception.getMessage()
                );
            }
        }


        // =====================================================
        // STEP 6: CHECK REACHABLE AMBULANCES
        // =====================================================

        if (priorityQueue.isEmpty()) {

            throw new RuntimeException(
                    "No reachable available ambulance found"
            );
        }


        // =====================================================
        // STEP 7: SELECT FASTEST AMBULANCE
        // =====================================================

        AmbulanceDistance best =
                priorityQueue.poll();


        Ambulance ambulance =
                best.ambulance();


        // =====================================================
        // STEP 8: RETURN RESULT
        // =====================================================

        return createResponse(
                emergency,
                ambulance,
                best.distanceKm(),
                best.travelTimeMinutes(),
                best.trafficLevel()
        );
    }


    // =========================================================
    // DISPATCH AMBULANCE
    // =========================================================
    //
    // AVAILABLE
    //     ↓
    // Traffic-aware Dijkstra
    //     ↓
    // PriorityQueue
    //     ↓
    // Fastest ambulance
    //     ↓
    // EN_ROUTE
    //
    // =========================================================

    public DispatchResponse dispatchAmbulance(
            Long emergencyId
    ) {

        // =====================================================
        // STEP 1: FIND EMERGENCY
        // =====================================================

        Emergency emergency =
                emergencyRepository.findById(
                                emergencyId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency not found with id: "
                                                + emergencyId
                                )
                        );


        // =====================================================
        // STEP 2: FIND NEAREST GRAPH NODE
        // =====================================================

        GraphNode emergencyNode =
                roadGraph.findNearestNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                );


        // =====================================================
        // STEP 3: GET ALL AMBULANCES
        // =====================================================

        List<Ambulance> ambulances =
                ambulanceRepository.findAll();


        // =====================================================
        // STEP 4: PRIORITY QUEUE
        // =====================================================

        PriorityQueue<AmbulanceDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                AmbulanceDistance::travelTimeMinutes
                        )
                );


        // =====================================================
        // STEP 5: EVALUATE AVAILABLE AMBULANCES
        // =====================================================

        for (Ambulance ambulance : ambulances) {

            if (!"AVAILABLE".equalsIgnoreCase(
                    ambulance.getStatus()
            )) {

                continue;
            }


            // -------------------------------------------------
            // Map ambulance GPS location to nearest OSM node.
            // -------------------------------------------------

            GraphNode ambulanceNode =
                    roadGraph.findNearestNode(
                            ambulance.getLatitude(),
                            ambulance.getLongitude()
                    );


            try {

                // -------------------------------------------------
                // Traffic-aware Dijkstra
                //
                // Ambulance → Emergency
                // -------------------------------------------------

                TrafficDijkstraResponse route =
                        trafficAwareDijkstraService
                                .findShortestPath(
                                        ambulanceNode.id(),
                                        emergencyNode.id()
                                );


                // -------------------------------------------------
                // Add candidate to PriorityQueue.
                // -------------------------------------------------

                priorityQueue.offer(
                        new AmbulanceDistance(
                                ambulance,
                                route.distanceKm(),
                                route.estimatedTravelTimeMinutes(),
                                route.trafficLevel()
                        )
                );

            } catch (RuntimeException exception) {

                System.out.println(
                        "No traffic-aware route found for ambulance "
                                + ambulance.getAmbulanceNumber()
                                + ": "
                                + exception.getMessage()
                );
            }
        }


        // =====================================================
        // STEP 6: CHECK AVAILABILITY
        // =====================================================

        if (priorityQueue.isEmpty()) {

            throw new RuntimeException(
                    "No reachable available ambulance found"
            );
        }


        // =====================================================
        // STEP 7: SELECT FASTEST AMBULANCE
        // =====================================================

        AmbulanceDistance best =
                priorityQueue.poll();


        Ambulance ambulance =
                best.ambulance();


        // =====================================================
        // STEP 8: CHANGE STATUS
        // =====================================================
        //
        // AVAILABLE → EN_ROUTE
        //
        // =====================================================

        ambulance.setStatus(
                "EN_ROUTE"
        );


        ambulance =
                ambulanceRepository.save(
                        ambulance
                );


        // =====================================================
        // STEP 9: RETURN COMPLETE RESULT
        // =====================================================

        return createResponse(
                emergency,
                ambulance,
                best.distanceKm(),
                best.travelTimeMinutes(),
                best.trafficLevel()
        );
    }


    // =========================================================
    // CREATE RESPONSE
    // =========================================================

    private DispatchResponse createResponse(

            Emergency emergency,

            Ambulance ambulance,

            double distanceKm,

            double travelTimeMinutes,

            String trafficLevel

    ) {

        return new DispatchResponse(

                emergency.getId(),

                ambulance.getId(),

                ambulance.getAmbulanceNumber(),

                ambulance.getType(),

                ambulance.getStatus(),

                roundToTwoDecimals(
                        distanceKm
                ),

                roundToTwoDecimals(
                        travelTimeMinutes
                ),

                trafficLevel

        );
    }


    // =========================================================
    // ROUND VALUE
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