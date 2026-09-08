package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchPlanResponse;
import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.dto.TripRoute;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    private final TrafficAwareDijkstraService
            trafficAwareDijkstraService;


    // =========================================================
    // CREATE COMPLETE DISPATCH PLAN
    // =========================================================
    //
    // Emergency
    //      ↓
    // Best ambulance
    //      ↓
    // Route ambulance → emergency
    //      ↓
    // Select hospital
    //      ↓
    // Route emergency → hospital
    //      ↓
    // Mark ambulance EN_ROUTE
    //      ↓
    // Return complete trip
    //
    // =========================================================

    @Transactional
    public DispatchPlanResponse createDispatchPlan(
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
        // STEP 2: FIND BEST AVAILABLE AMBULANCE
        // =====================================================

        DispatchResponse ambulanceResponse =
                dispatchService.findBestAmbulance(
                        emergencyId
                );


        // =====================================================
        // STEP 3: LOAD AMBULANCE
        // =====================================================

        Ambulance ambulance =
                ambulanceRepository.findById(
                                ambulanceResponse.ambulanceId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Selected ambulance not found"
                                )
                        );


        // =====================================================
        // STEP 4: FIND BEST HOSPITAL
        // =====================================================

        HospitalSelectionResponse hospitalResponse =
                hospitalSelectionService.findBestHospital(
                        emergencyId
                );


        // =====================================================
        // STEP 5: LOAD HOSPITAL
        // =====================================================

        Hospital hospital =
                hospitalRepository.findById(
                                hospitalResponse.hospitalId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Selected hospital not found"
                                )
                        );


        // =====================================================
        // STEP 6: ROUTE AMBULANCE → EMERGENCY
        // =====================================================

        GraphNode ambulanceNode =
                roadGraph.findNearestNode(
                        ambulance.getLatitude(),
                        ambulance.getLongitude()
                );


        if (ambulanceNode == null) {

            throw new RuntimeException(
                    "Unable to find road node near ambulance"
            );
        }


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


        var ambulanceToEmergency =
                trafficAwareDijkstraService
                        .findShortestPath(
                                ambulanceNode.id(),
                                emergencyNode.id()
                        );


        TripRoute.RouteSegment
                routeToEmergency =
                createRouteSegment(
                        ambulanceToEmergency
                );


        // =====================================================
        // STEP 7: ROUTE EMERGENCY → HOSPITAL
        // =====================================================
        //
        // This is the second phase of the trip.
        //
        // =====================================================

        GraphNode hospitalNode =
                roadGraph.findNearestNode(
                        hospital.getLatitude(),
                        hospital.getLongitude()
                );


        if (hospitalNode == null) {

            throw new RuntimeException(
                    "Unable to find road node near hospital"
            );
        }


        var emergencyToHospital =
                trafficAwareDijkstraService
                        .findShortestPath(
                                emergencyNode.id(),
                                hospitalNode.id()
                        );


        TripRoute.RouteSegment
                routeToHospital =
                createRouteSegment(
                        emergencyToHospital
                );


        // =====================================================
        // STEP 8: CALCULATE EXISTING FINAL ROUTE
        // =====================================================
        //
        // Keep the existing route response so your current
        // dashboard remains compatible.
        //
        // =====================================================

        var routeResponse =
                routeService.findRoute(
                        ambulance.getId(),
                        hospital.getId()
                );


        // =====================================================
        // STEP 9: ALL ROUTES SUCCESSFUL
        // =====================================================
        //
        // Only now change:
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
        // STEP 10: CREATE TRIP
        // =====================================================

        TripRoute trip =
                new TripRoute(

                        TripRoute.TripStatus
                                .TO_EMERGENCY,

                        routeToEmergency,

                        routeToHospital

                );


        // =====================================================
        // STEP 11: RETURN COMPLETE PLAN
        // =====================================================

        return new DispatchPlanResponse(

                emergency.getId(),


                // -------------------------------------------------
                // AMBULANCE
                // -------------------------------------------------

                new DispatchPlanResponse
                        .AmbulanceDetails(

                        ambulance.getId(),

                        ambulance
                                .getAmbulanceNumber(),

                        ambulance.getType(),

                        ambulance.getStatus(),

                        ambulanceResponse
                                .distanceKm(),

                        ambulanceResponse
                                .estimatedTravelTimeMinutes(),

                        ambulanceResponse
                                .trafficLevel()

                ),


                // -------------------------------------------------
                // HOSPITAL
                // -------------------------------------------------

                new DispatchPlanResponse
                        .HospitalDetails(

                        hospital.getId(),

                        hospital.getHospitalCode(),

                        hospital.getName(),

                        hospital.getFacilityType(),

                        hospital.getAvailableBeds(),

                        hospitalResponse
                                .distanceKm(),

                        hospitalResponse
                                .estimatedTravelTimeMinutes(),

                        hospitalResponse
                                .trafficLevel()

                ),


                // -------------------------------------------------
                // EXISTING ROUTE
                // -------------------------------------------------

                new DispatchPlanResponse
                        .RouteDetails(

                        routeResponse
                                .sourceNode(),

                        routeResponse
                                .destinationNode(),

                        routeResponse
                                .distanceKm(),

                        routeResponse
                                .estimatedTravelTimeMinutes(),

                        routeResponse
                                .trafficLevel(),

                        routeResponse
                                .nodePath()

                ),


                // -------------------------------------------------
                // COMPLETE TRIP
                // -------------------------------------------------

                trip

        );
    }


    // =========================================================
    // CREATE ROUTE SEGMENT
    // =========================================================

    private TripRoute.RouteSegment
    createRouteSegment(
            com.ambulanceos.dto.TrafficDijkstraResponse
                    response
    ) {

        List<TripRoute.RoutePoint>
                coordinates =
                new ArrayList<>();


        for (
                String nodeId :
                response.path()
        ) {

            GraphNode node =
                    roadGraph.getNode(
                            nodeId
                    );


            if (node == null) {

                throw new RuntimeException(
                        "Route node not found: "
                                + nodeId
                );
            }


            coordinates.add(
                    new TripRoute.RoutePoint(

                            node.latitude(),

                            node.longitude()

                    )
            );
        }


        return new TripRoute.RouteSegment(

                response.distanceKm(),

                response
                        .estimatedTravelTimeMinutes(),

                response.trafficLevel(),

                response.path(),

                coordinates

        );
    }
}