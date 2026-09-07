package com.ambulanceos.service;

import com.ambulanceos.dto.DijkstraResponse;
import com.ambulanceos.dto.DispatchResponse;
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

    private final DijkstraService dijkstraService;


    // =========================================================
    // FIND BEST AVAILABLE AMBULANCE
    // =========================================================
    //
    // Algorithm:
    //
    // Emergency
    //     ↓
    // AVAILABLE ambulances
    //     ↓
    // Map ambulance to nearest graph node
    //     ↓
    // Map emergency to nearest graph node
    //     ↓
    // Dijkstra shortest road distance
    //     ↓
    // PriorityQueue
    //     ↓
    // Best ambulance
    //
    // =========================================================

    public DispatchResponse findBestAmbulance(
            Long emergencyId
    ) {

        // -----------------------------------------------------
        // 1. Find emergency
        // -----------------------------------------------------

        Emergency emergency =
                emergencyRepository.findById(emergencyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency not found with id: "
                                                + emergencyId
                                )
                        );


        // -----------------------------------------------------
        // 2. Find nearest graph node to emergency
        // -----------------------------------------------------

        GraphNode emergencyNode =
                findNearestGraphNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                );


        // -----------------------------------------------------
        // 3. Get all ambulances
        // -----------------------------------------------------

        List<Ambulance> ambulances =
                ambulanceRepository.findAll();


        // -----------------------------------------------------
        // 4. Create PriorityQueue
        //
        // Ambulance with smallest Dijkstra distance
        // gets highest priority.
        // -----------------------------------------------------

        PriorityQueue<AmbulanceDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                AmbulanceDistance::distanceKm
                        )
                );


        // -----------------------------------------------------
        // 5. Calculate Dijkstra distance for every
        //    AVAILABLE ambulance
        // -----------------------------------------------------

        for (Ambulance ambulance : ambulances) {

            if (!"AVAILABLE".equalsIgnoreCase(
                    ambulance.getStatus()
            )) {

                continue;
            }


            // Find graph node nearest to ambulance

            GraphNode ambulanceNode =
                    findNearestGraphNode(
                            ambulance.getLatitude(),
                            ambulance.getLongitude()
                    );


            try {

                // -------------------------------------------------
                // Run Dijkstra
                // -------------------------------------------------

                DijkstraResponse route =
                        dijkstraService.findShortestPath(
                                ambulanceNode.id(),
                                emergencyNode.id()
                        );


                double distanceKm =
                        route.distanceKm();


                // -------------------------------------------------
                // Add ambulance to PriorityQueue
                // -------------------------------------------------

                priorityQueue.offer(
                        new AmbulanceDistance(
                                ambulance,
                                distanceKm
                        )
                );


            } catch (RuntimeException exception) {

                // -------------------------------------------------
                // If no graph route exists for this ambulance,
                // skip it and continue checking other ambulances.
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
        // 6. Check whether an ambulance is available
        //    and reachable
        // -----------------------------------------------------

        if (priorityQueue.isEmpty()) {

            throw new RuntimeException(
                    "No reachable available ambulance found"
            );
        }


        // -----------------------------------------------------
        // 7. Get ambulance with smallest Dijkstra distance
        // -----------------------------------------------------

        AmbulanceDistance best =
                priorityQueue.poll();


        Ambulance ambulance =
                best.ambulance();


        // -----------------------------------------------------
        // 8. Return result
        // -----------------------------------------------------

        return createResponse(
                emergency,
                ambulance,
                best.distanceKm()
        );
    }


    // =========================================================
    // DISPATCH AMBULANCE
    // =========================================================
    //
    // This method actually changes:
    //
    // AVAILABLE
    //      ↓
    // EN_ROUTE
    //
    // The ambulance selected using Dijkstra is then dispatched.
    //
    // =========================================================

    public DispatchResponse dispatchAmbulance(
            Long emergencyId
    ) {

        // -----------------------------------------------------
        // 1. Find emergency
        // -----------------------------------------------------

        Emergency emergency =
                emergencyRepository.findById(emergencyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency not found with id: "
                                                + emergencyId
                                )
                        );


        // -----------------------------------------------------
        // 2. Find nearest graph node to emergency
        // -----------------------------------------------------

        GraphNode emergencyNode =
                findNearestGraphNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                );


        // -----------------------------------------------------
        // 3. Get all ambulances
        // -----------------------------------------------------

        List<Ambulance> ambulances =
                ambulanceRepository.findAll();


        // -----------------------------------------------------
        // 4. Create PriorityQueue
        // -----------------------------------------------------

        PriorityQueue<AmbulanceDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                AmbulanceDistance::distanceKm
                        )
                );


        // -----------------------------------------------------
        // 5. Calculate Dijkstra distance for AVAILABLE
        //    ambulances
        // -----------------------------------------------------

        for (Ambulance ambulance : ambulances) {

            if (!"AVAILABLE".equalsIgnoreCase(
                    ambulance.getStatus()
            )) {

                continue;
            }


            // Find nearest graph node to ambulance

            GraphNode ambulanceNode =
                    findNearestGraphNode(
                            ambulance.getLatitude(),
                            ambulance.getLongitude()
                    );


            try {

                // -------------------------------------------------
                // Run Dijkstra from ambulance → emergency
                // -------------------------------------------------

                DijkstraResponse route =
                        dijkstraService.findShortestPath(
                                ambulanceNode.id(),
                                emergencyNode.id()
                        );


                double distanceKm =
                        route.distanceKm();


                // Add to PriorityQueue

                priorityQueue.offer(
                        new AmbulanceDistance(
                                ambulance,
                                distanceKm
                        )
                );


            } catch (RuntimeException exception) {

                System.out.println(
                        "No route found for ambulance "
                                + ambulance.getAmbulanceNumber()
                                + ": "
                                + exception.getMessage()
                );
            }
        }


        // -----------------------------------------------------
        // 6. Check availability
        // -----------------------------------------------------

        if (priorityQueue.isEmpty()) {

            throw new RuntimeException(
                    "No reachable available ambulance found"
            );
        }


        // -----------------------------------------------------
        // 7. Select best ambulance
        // -----------------------------------------------------

        AmbulanceDistance best =
                priorityQueue.poll();


        Ambulance ambulance =
                best.ambulance();


        // -----------------------------------------------------
        // 8. Change ambulance status
        //
        // AVAILABLE → EN_ROUTE
        // -----------------------------------------------------

        ambulance.setStatus(
                "EN_ROUTE"
        );


        ambulance =
                ambulanceRepository.save(
                        ambulance
                );


        // -----------------------------------------------------
        // 9. Return dispatch result
        // -----------------------------------------------------

        return createResponse(
                emergency,
                ambulance,
                best.distanceKm()
        );
    }


    // =========================================================
    // FIND NEAREST GRAPH NODE
    // =========================================================
    //
    // Converts a real-world latitude/longitude location
    // into the closest node in our Gurgaon road graph.
    //
    // =========================================================

    private GraphNode findNearestGraphNode(

            double latitude,

            double longitude

    ) {

        GraphNode nearestNode = null;

        double smallestDistance =
                Double.POSITIVE_INFINITY;


        for (GraphNode node :
                roadGraph.getNodes().values()) {

            double distance =
                    calculateHaversineDistance(

                            latitude,
                            longitude,

                            node.latitude(),
                            node.longitude()

                    );


            if (distance < smallestDistance) {

                smallestDistance =
                        distance;

                nearestNode =
                        node;
            }
        }


        if (nearestNode == null) {

            throw new RuntimeException(
                    "Unable to find nearest graph node"
            );
        }


        return nearestNode;
    }


    // =========================================================
    // CREATE RESPONSE
    // =========================================================

    private DispatchResponse createResponse(

            Emergency emergency,

            Ambulance ambulance,

            double distanceKm

    ) {

        return new DispatchResponse(

                emergency.getId(),

                ambulance.getId(),

                ambulance.getAmbulanceNumber(),

                ambulance.getType(),

                ambulance.getStatus(),

                roundToTwoDecimals(
                        distanceKm
                )

        );
    }


    // =========================================================
    // HAVERSINE DISTANCE
    // =========================================================
    //
    // Haversine is ONLY used to connect a real location
    // to the nearest node in our graph.
    //
    // It is NOT used for route optimization.
    //
    // Actual route distance comes from Dijkstra.
    //
    // =========================================================

    private double calculateHaversineDistance(

            double latitude1,
            double longitude1,

            double latitude2,
            double longitude2

    ) {

        final double EARTH_RADIUS_KM =
                6371.0;


        double lat1 =
                Math.toRadians(
                        latitude1
                );

        double lat2 =
                Math.toRadians(
                        latitude2
                );


        double deltaLatitude =
                Math.toRadians(
                        latitude2 - latitude1
                );

        double deltaLongitude =
                Math.toRadians(
                        longitude2 - longitude1
                );


        double a =

                Math.sin(
                        deltaLatitude / 2
                )
                        *
                        Math.sin(
                                deltaLatitude / 2
                        )

                        +

                        Math.cos(lat1)
                                *
                                Math.cos(lat2)
                                *
                                Math.sin(
                                        deltaLongitude / 2
                                )
                                *
                                Math.sin(
                                        deltaLongitude / 2
                                );


        double c =
                2 * Math.atan2(

                        Math.sqrt(a),

                        Math.sqrt(1 - a)

                );


        return EARTH_RADIUS_KM * c;
    }


    // =========================================================
    // ROUND DISTANCE
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

            double distanceKm

    ) {
    }
}