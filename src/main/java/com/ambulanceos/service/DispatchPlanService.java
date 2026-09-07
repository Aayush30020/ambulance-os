package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchPlanResponse;
import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.EmergencyRepository;
import com.ambulanceos.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DispatchPlanService {

    private final EmergencyRepository emergencyRepository;

    private final AmbulanceRepository ambulanceRepository;

    private final HospitalRepository hospitalRepository;

    private final DispatchService dispatchService;

    private final HospitalSelectionService hospitalSelectionService;

    private final RouteService routeService;

    private final GurgaonRoadGraph roadGraph;


    public DispatchPlanResponse createDispatchPlan(
            Long emergencyId
    ) {

        // =========================================================
        // STEP 1: Find the emergency
        // =========================================================

        Emergency emergency =
                emergencyRepository.findById(emergencyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Emergency not found with id: "
                                                + emergencyId
                                )
                        );


        // =========================================================
        // STEP 2: Find and dispatch the best ambulance
        //
        // DispatchService uses:
        // - AVAILABLE ambulances
        // - Haversine distance to nearest graph node
        // - Dijkstra shortest path
        // - PriorityQueue
        //
        // The selected ambulance becomes EN_ROUTE.
        // =========================================================

        DispatchResponse ambulanceResponse =
                dispatchService.dispatchAmbulance(
                        emergencyId
                );


        // =========================================================
        // STEP 3: Find the best hospital
        //
        // HospitalSelectionService considers:
        // - Available beds
        // - Required facility
        // - Dijkstra shortest path
        // - PriorityQueue
        // =========================================================

        HospitalSelectionResponse hospitalResponse =
                hospitalSelectionService.findBestHospital(
                        emergencyId
                );


        // =========================================================
        // STEP 4: Load the selected ambulance
        // =========================================================

        Ambulance ambulance =
                ambulanceRepository.findById(
                                ambulanceResponse.ambulanceId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Selected ambulance not found"
                                )
                        );


        // =========================================================
        // STEP 5: Load the selected hospital
        // =========================================================

        Hospital hospital =
                hospitalRepository.findById(
                                hospitalResponse.hospitalId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Selected hospital not found"
                                )
                        );


        // =========================================================
        // STEP 6: Calculate ambulance → hospital route
        //
        // RouteService:
        // - Finds nearest graph node to ambulance
        // - Finds nearest graph node to hospital
        // - Runs Dijkstra
        // - Returns route coordinates
        // =========================================================

        var routeResponse =
                routeService.findRoute(
                        ambulance.getId(),
                        hospital.getId()
                );


        // =========================================================
        // STEP 7: Convert route coordinates into graph node IDs
        //
        // The ambulance's exact GPS location may NOT be a graph
        // node. Therefore findNodeId() can return "UNKNOWN".
        //
        // We remove UNKNOWN values because the Dijkstra route
        // itself starts from the nearest graph node.
        // =========================================================

        var routeNodes =
                routeResponse.route()
                        .stream()
                        .map(point ->
                                findNodeId(
                                        point.latitude(),
                                        point.longitude()
                                )
                        )
                        .filter(nodeId ->
                                !"UNKNOWN".equals(nodeId)
                        )
                        .toList();


        // =========================================================
        // STEP 8: Create the complete dispatch plan response
        // =========================================================

        return new DispatchPlanResponse(

                emergency.getId(),

                // -------------------------------------------------
                // Selected ambulance
                // -------------------------------------------------

                new DispatchPlanResponse.AmbulanceDetails(
                        ambulance.getId(),
                        ambulance.getAmbulanceNumber(),
                        ambulance.getType(),
                        ambulance.getStatus(),
                        ambulanceResponse.distanceKm()
                ),

                // -------------------------------------------------
                // Selected hospital
                // -------------------------------------------------

                new DispatchPlanResponse.HospitalDetails(
                        hospital.getId(),
                        hospital.getHospitalCode(),
                        hospital.getName(),
                        hospital.getFacilityType(),
                        hospital.getAvailableBeds(),
                        hospitalResponse.distanceKm()
                ),

                // -------------------------------------------------
                // Optimized route
                // -------------------------------------------------

                new DispatchPlanResponse.RouteDetails(
                        routeResponse.sourceNode(),
                        routeResponse.destinationNode(),
                        routeResponse.distanceKm(),
                        routeNodes
                )
        );
    }


    // =============================================================
    // Find the graph node corresponding to route coordinates
    // =============================================================

    private String findNodeId(
            double latitude,
            double longitude
    ) {

        /*
         * Floating-point numbers should not normally be compared
         * using exact equality.
         *
         * Example:
         *
         * 28.4595000001
         *
         * and
         *
         * 28.4595
         *
         * represent practically the same coordinate, but Java's
         * exact comparison may consider them different.
         *
         * Therefore we use a small tolerance.
         */

        final double EPSILON = 0.000001;


        for (GraphNode node :
                roadGraph.getNodes().values()) {

            boolean latitudeMatches =
                    Math.abs(
                            node.latitude() - latitude
                    ) < EPSILON;


            boolean longitudeMatches =
                    Math.abs(
                            node.longitude() - longitude
                    ) < EPSILON;


            if (latitudeMatches
                    && longitudeMatches) {

                return node.id();
            }
        }


        /*
         * This can happen when the coordinate represents the
         * ambulance's actual GPS location rather than a graph node.
         */
        return "UNKNOWN";
    }
}