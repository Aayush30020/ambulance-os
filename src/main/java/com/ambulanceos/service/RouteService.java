package com.ambulanceos.service;

import com.ambulanceos.dto.RouteResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.exception.AmbulanceNotFoundException;
import com.ambulanceos.exception.HospitalNotFoundException;
import com.ambulanceos.exception.RouteNotFoundException;
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

    /*
     * Central routing gateway.
     *
     * RoutingService reads the currently selected algorithm
     * from PostgreSQL.
     *
     * Therefore this route can use:
     *
     *      DIJKSTRA
     *
     * or
     *
     *      ASTAR
     */
    private final RoutingService routingService;


    // =========================================================
    // FIND AMBULANCE → HOSPITAL ROUTE
    // =========================================================
    //
    // Flow:
    //
    // Ambulance
    //      ↓
    // Find nearest OSM road node
    //      ↓
    // RoutingService
    //      ↓
    // Selected algorithm
    //      ↓
    // Hospital
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
                ).orElseThrow(() ->
                        new AmbulanceNotFoundException(
                                ambulanceId
                        )
                );


        // =====================================================
        // 2. FIND HOSPITAL
        // =====================================================

        Hospital hospital =
                hospitalRepository.findById(
                        hospitalId
                ).orElseThrow(() ->
                        new HospitalNotFoundException(
                                hospitalId
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

            throw new RouteNotFoundException(
                    "Unable to find road node near ambulance "
                            + ambulance.getAmbulanceNumber()
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

            throw new RouteNotFoundException(
                    "Unable to find road node near hospital "
                            + hospital.getName()
            );
        }


        // =====================================================
        // 5. RUN SELECTED ROUTING ALGORITHM
        // =====================================================
        //
        // RoutingService automatically determines the current
        // routing algorithm from the system settings.
        //
        // DIJKSTRA:
        //      Traffic-aware Dijkstra
        //
        // ASTAR:
        //      A*
        //
        // =====================================================

        TrafficDijkstraResponse trafficRoute;

        try {

            trafficRoute =
                    routingService.findRoute(
                            sourceNode.id(),
                            destinationNode.id()
                    );

        } catch (RuntimeException exception) {

            throw new RouteNotFoundException(
                    "No route found between ambulance "
                            + ambulance.getAmbulanceNumber()
                            + " and hospital "
                            + hospital.getName()
                            + ": "
                            + exception.getMessage()
            );
        }


        // =====================================================
        // 6. GET NODE PATH
        // =====================================================

        List<String> nodePath =
                trafficRoute.path();


        if (
                nodePath == null
                        || nodePath.isEmpty()
        ) {

            throw new RouteNotFoundException(
                    "Routing algorithm returned an empty route"
            );
        }


        // =====================================================
        // 7. CONVERT GRAPH NODES TO LATITUDE/LONGITUDE
        // =====================================================
        //
        // Leaflet requires:
        //
        //      [latitude, longitude]
        //
        // for every point in the route.
        //
        // =====================================================

        List<RouteResponse.RoutePoint> route =
                new ArrayList<>();


        for (String nodeId :
                nodePath) {

            GraphNode node =
                    roadGraph.getNode(
                            nodeId
                    );


            if (node == null) {

                throw new RouteNotFoundException(
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
        // 8. GET ACTUAL SELECTED ALGORITHM
        // =====================================================

        RoutingAlgorithm selectedAlgorithm =
                routingService.getCurrentAlgorithm();


        // =====================================================
        // 9. LOG ROUTE INFORMATION
        // =====================================================

        System.out.println();

        System.out.println(
                "=========================================="
        );

        System.out.println(
                " Ambulance → Hospital Route"
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
                "Algorithm: "
                        + selectedAlgorithm
        );

        System.out.println(
                "=========================================="
        );

        System.out.println();


        // =====================================================
        // 10. RETURN COMPLETE ROUTE
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