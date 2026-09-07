package com.ambulanceos.service;

import com.ambulanceos.dto.DijkstraResponse;
import com.ambulanceos.dto.RouteResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final AmbulanceRepository ambulanceRepository;

    private final HospitalRepository hospitalRepository;

    private final GurgaonRoadGraph roadGraph;

    private final DijkstraService dijkstraService;


    // =========================================================
    // FIND ROUTE FROM AMBULANCE TO HOSPITAL
    // =========================================================
    //
    // Flow:
    //
    // Ambulance coordinates
    //        ↓
    // Find nearest graph node
    //        ↓
    // Dijkstra shortest path
    //        ↓
    // Hospital graph node
    //        ↓
    // Hospital coordinates
    //
    // =========================================================

    public RouteResponse findRoute(
            Long ambulanceId,
            Long hospitalId
    ) {

        // -----------------------------------------------------
        // 1. Find ambulance from PostgreSQL
        // -----------------------------------------------------

        Ambulance ambulance =
                ambulanceRepository.findById(ambulanceId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Ambulance not found with id: "
                                                + ambulanceId
                                )
                        );


        // -----------------------------------------------------
        // 2. Find hospital from PostgreSQL
        // -----------------------------------------------------

        Hospital hospital =
                hospitalRepository.findById(hospitalId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Hospital not found with id: "
                                                + hospitalId
                                )
                        );


        // -----------------------------------------------------
        // 3. Find nearest graph node to ambulance
        // -----------------------------------------------------
        //
        // The ambulance has real GPS coordinates.
        //
        // Our Dijkstra algorithm works on graph nodes.
        //
        // Therefore we map the real ambulance location
        // to the closest graph node.
        // -----------------------------------------------------

        GraphNode sourceNode =
                findNearestGraphNode(
                        ambulance.getLatitude(),
                        ambulance.getLongitude()
                );


        // -----------------------------------------------------
        // 4. Find nearest graph node to hospital
        // -----------------------------------------------------

        GraphNode destinationNode =
                findNearestGraphNode(
                        hospital.getLatitude(),
                        hospital.getLongitude()
                );


        // -----------------------------------------------------
        // 5. Run Dijkstra
        // -----------------------------------------------------
        //
        // Dijkstra calculates the shortest path between
        // the two graph nodes.
        //
        // Example:
        //
        // G01 → G09 → G10
        //
        // -----------------------------------------------------

        DijkstraResponse dijkstraResponse =
                dijkstraService.findShortestPath(

                        sourceNode.id(),

                        destinationNode.id()

                );


        // -----------------------------------------------------
        // 6. Create route coordinate list
        // -----------------------------------------------------

        List<RouteResponse.RoutePoint> route =
                new ArrayList<>();


        // -----------------------------------------------------
        // 7. Add ACTUAL ambulance location
        // -----------------------------------------------------
        //
        // This makes the route begin exactly where the
        // ambulance is stored in PostgreSQL.
        // -----------------------------------------------------

        addRoutePointIfDifferent(

                route,

                ambulance.getLatitude(),

                ambulance.getLongitude()

        );


        // -----------------------------------------------------
        // 8. Add Dijkstra graph path
        // -----------------------------------------------------
        //
        // Convert:
        //
        // G01 → G09 → G10
        //
        // into:
        //
        // latitude / longitude coordinates
        //
        // -----------------------------------------------------

        for (String nodeId :
                dijkstraResponse.path()) {

            GraphNode node =
                    roadGraph.getNode(nodeId);


            if (node == null) {

                throw new RuntimeException(
                        "Graph node not found: " + nodeId
                );
            }


            addRoutePointIfDifferent(

                    route,

                    node.latitude(),

                    node.longitude()

            );
        }


        // -----------------------------------------------------
        // 9. Add ACTUAL hospital location
        // -----------------------------------------------------
        //
        // This makes the route finish exactly at the
        // hospital coordinates stored in PostgreSQL.
        // -----------------------------------------------------

        addRoutePointIfDifferent(

                route,

                hospital.getLatitude(),

                hospital.getLongitude()

        );


        // -----------------------------------------------------
        // 10. Return complete route response
        // -----------------------------------------------------

        return new RouteResponse(

                ambulance.getId(),

                ambulance.getAmbulanceNumber(),

                hospital.getId(),

                hospital.getName(),

                sourceNode.id(),

                destinationNode.id(),

                dijkstraResponse.distanceKm(),

                route

        );
    }


    // =========================================================
    // FIND NEAREST GRAPH NODE
    // =========================================================
    //
    // Compare a real-world latitude/longitude against every
    // node in the Gurgaon road graph.
    //
    // The node with the smallest geographic distance wins.
    //
    // This is what connects our database locations with
    // our graph-based Dijkstra algorithm.
    //
    // =========================================================

    private GraphNode findNearestGraphNode(

            double latitude,

            double longitude

    ) {

        GraphNode nearestNode = null;

        double smallestDistance =
                Double.POSITIVE_INFINITY;


        // -----------------------------------------------------
        // Check every graph node
        // -----------------------------------------------------

        for (GraphNode node :
                roadGraph.getNodes().values()) {

            double distance =
                    calculateDistance(

                            latitude,
                            longitude,

                            node.latitude(),
                            node.longitude()

                    );


            // -------------------------------------------------
            // If this node is closer, make it the new nearest
            // node.
            // -------------------------------------------------

            if (distance < smallestDistance) {

                smallestDistance = distance;

                nearestNode = node;
            }
        }


        // -----------------------------------------------------
        // Safety check
        // -----------------------------------------------------

        if (nearestNode == null) {

            throw new RuntimeException(
                    "Unable to find nearest graph node"
            );
        }


        return nearestNode;
    }


    // =========================================================
    // ADD ROUTE POINT
    // =========================================================
    //
    // Prevent duplicate consecutive coordinates.
    //
    // For example:
    //
    // Ambulance
    //     ↓
    // G01
    //
    // If ambulance coordinates are exactly G01 coordinates,
    // we don't add the same point twice.
    //
    // =========================================================

    private void addRoutePointIfDifferent(

            List<RouteResponse.RoutePoint> route,

            double latitude,

            double longitude

    ) {

        if (route.isEmpty()) {

            route.add(
                    new RouteResponse.RoutePoint(
                            latitude,
                            longitude
                    )
            );

            return;
        }


        RouteResponse.RoutePoint lastPoint =
                route.get(
                        route.size() - 1
                );


        // -----------------------------------------------------
        // Compare coordinates with very small tolerance.
        // -----------------------------------------------------

        double latitudeDifference =
                Math.abs(
                        lastPoint.latitude()
                                - latitude
                );


        double longitudeDifference =
                Math.abs(
                        lastPoint.longitude()
                                - longitude
                );


        if (latitudeDifference > 0.000001
                || longitudeDifference > 0.000001) {

            route.add(
                    new RouteResponse.RoutePoint(
                            latitude,
                            longitude
                    )
            );
        }
    }


    // =========================================================
    // HAVERSINE DISTANCE
    // =========================================================
    //
    // Calculates geographic distance between two coordinates.
    //
    // Used to determine which graph node is closest to the
    // ambulance/hospital.
    //
    // =========================================================

    private double calculateDistance(

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
}