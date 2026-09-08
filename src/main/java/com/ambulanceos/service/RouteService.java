package com.ambulanceos.service;

import com.ambulanceos.dto.RouteResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
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

    private final TrafficAwareDijkstraService
            trafficAwareDijkstraService;


    // =========================================================
    // FIND TRAFFIC-AWARE AMBULANCE → HOSPITAL ROUTE
    // =========================================================
    //
    // Flow:
    //
    // Ambulance
    //      ↓
    // Find nearest OSM road node
    //      ↓
    // Traffic-Aware Dijkstra
    //      ↓
    // Hospital
    //      ↓
    // Traffic-optimized path
    //      ↓
    // Convert OSM nodes to coordinates
    //      ↓
    // Return route for Leaflet
    //
    // =========================================================

    public RouteResponse findRoute(

            Long ambulanceId,

            Long hospitalId

    ) {

        // =====================================================
        // 1. FIND AMBULANCE
        // =====================================================

        Ambulance ambulance =
                ambulanceRepository.findById(
                                ambulanceId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Ambulance not found with id: "
                                                + ambulanceId
                                )
                        );


        // =====================================================
        // 2. FIND HOSPITAL
        // =====================================================

        Hospital hospital =
                hospitalRepository.findById(
                                hospitalId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Hospital not found with id: "
                                                + hospitalId
                                )
                        );


        // =====================================================
        // 3. FIND NEAREST OSM NODE TO AMBULANCE
        // =====================================================

        GraphNode sourceNode =
                roadGraph.findNearestNode(
                        ambulance.getLatitude(),
                        ambulance.getLongitude()
                );


        if (sourceNode == null) {

            throw new RuntimeException(
                    "Unable to find road node near ambulance"
            );
        }


        // =====================================================
        // 4. FIND NEAREST OSM NODE TO HOSPITAL
        // =====================================================

        GraphNode destinationNode =
                roadGraph.findNearestNode(
                        hospital.getLatitude(),
                        hospital.getLongitude()
                );


        if (destinationNode == null) {

            throw new RuntimeException(
                    "Unable to find road node near hospital"
            );
        }


        // =====================================================
        // 5. RUN TRAFFIC-AWARE DIJKSTRA
        // =====================================================
        //
        // Instead of normal Dijkstra:
        //
        //     distance only
        //
        // we now use:
        //
        //     traffic-adjusted route cost
        //
        // The TrafficAwareDijkstraService returns:
        //
        //     physical distance
        //     estimated travel time
        //     traffic level
        //     optimized node path
        //
        // =====================================================

        TrafficDijkstraResponse trafficRoute =
                trafficAwareDijkstraService
                        .findShortestPath(
                                sourceNode.id(),
                                destinationNode.id()
                        );


        // =====================================================
        // 6. GET NODE PATH
        // =====================================================

        List<String> nodePath =
                trafficRoute.path();


        // =====================================================
        // 7. CONVERT GRAPH NODES TO LATITUDE/LONGITUDE
        // =====================================================
        //
        // Leaflet needs:
        //
        //     [latitude, longitude]
        //
        // for every point in the route.
        //
        // =====================================================

        List<RouteResponse.RoutePoint> route =
                new ArrayList<>();


        for (
                String nodeId :
                nodePath
        ) {

            GraphNode node =
                    roadGraph.getNode(
                            nodeId
                    );


            if (node == null) {

                throw new RuntimeException(
                        "Route node not found in graph: "
                                + nodeId
                );
            }


            route.add(
                    new RouteResponse.RoutePoint(

                            node.latitude(),

                            node.longitude()

                    )
            );
        }


        // =====================================================
        // 8. LOG ROUTE INFORMATION
        // =====================================================

        System.out.println();

        System.out.println(
                "=========================================="
        );

        System.out.println(
                " Traffic-Aware Ambulance → Hospital Route"
        );

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Ambulance: "
                        + ambulance.getAmbulanceNumber()
        );

        System.out.println(
                "Hospital: "
                        + hospital.getName()
        );

        System.out.println(
                "Source Node: "
                        + sourceNode.id()
        );

        System.out.println(
                "Destination Node: "
                        + destinationNode.id()
        );

        System.out.println(
                "Route Distance: "
                        + trafficRoute.distanceKm()
                        + " km"
        );

        System.out.println(
                "Estimated Travel Time: "
                        + trafficRoute
                        .estimatedTravelTimeMinutes()
                        + " min"
        );

        System.out.println(
                "Traffic Level: "
                        + trafficRoute.trafficLevel()
        );

        System.out.println(
                "Graph Nodes: "
                        + nodePath.size()
        );

        System.out.println(
                "Algorithm: Traffic-Aware Dijkstra"
        );

        System.out.println(
                "=========================================="
        );

        System.out.println();


        // =====================================================
        // 9. RETURN COMPLETE ROUTE
        // =====================================================

        return new RouteResponse(

                ambulance.getId(),

                ambulance.getAmbulanceNumber(),

                hospital.getId(),

                hospital.getName(),

                sourceNode.id(),

                destinationNode.id(),

                trafficRoute.distanceKm(),

                trafficRoute
                        .estimatedTravelTimeMinutes(),

                trafficRoute.trafficLevel(),

                nodePath,

                route

        );
    }
}