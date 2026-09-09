package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchPlanResponse;
import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.dto.TripRoute;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.repository.EmergencyRepository;
import com.ambulanceos.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DispatchPlanService {

    private final EmergencyRepository emergencyRepository;

    private final AmbulanceRepository ambulanceRepository;

    private final HospitalRepository hospitalRepository;

    private final DispatchRepository dispatchRepository;

    private final DispatchService dispatchService;

    private final HospitalSelectionService hospitalSelectionService;

    private final RouteService routeService;

    private final GurgaonRoadGraph roadGraph;

    /*
     * Central routing gateway.
     *
     * RoutingService automatically uses the algorithm
     * currently selected in PostgreSQL:
     *
     * DIJKSTRA
     * or
     * ASTAR
     */
    private final RoutingService routingService;

    /*
     * Used to store the exact algorithm that was selected
     * when this dispatch was created.
     */
    private final RoutingSettingsService routingSettingsService;


    // =========================================================
    // CREATE COMPLETE DISPATCH PLAN
    // =========================================================

    @Transactional
    public DispatchPlanResponse createDispatchPlan(
            Long emergencyId
    ) {

        // =====================================================
        // 1. FIND EMERGENCY
        // =====================================================

        Emergency emergency =
                emergencyRepository.findById(
                        emergencyId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Emergency not found with id: "
                                        + emergencyId
                        )
                );


        // =====================================================
        // 2. GET CURRENT ROUTING ALGORITHM
        // =====================================================
        //
        // The selected algorithm is read once at the beginning
        // of the dispatch.
        //
        // This ensures that the dispatch record stores the
        // algorithm that was actually selected for this trip.
        //
        // =====================================================

        RoutingAlgorithm selectedAlgorithm =
                routingSettingsService
                        .getRoutingAlgorithm();


        // =====================================================
        // 3. FIND BEST AVAILABLE AMBULANCE
        // =====================================================
        //
        // DispatchService uses RoutingService internally.
        //
        // Therefore it uses the same routing algorithm selected
        // in Settings.
        //
        // =====================================================

        DispatchResponse ambulanceResponse =
                dispatchService.findBestAmbulance(
                        emergencyId
                );


        // =====================================================
        // 4. LOAD SELECTED AMBULANCE
        // =====================================================

        Ambulance ambulance =
                ambulanceRepository.findById(
                        ambulanceResponse.ambulanceId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Selected ambulance not found"
                        )
                );


        // =====================================================
        // 5. FIND BEST SUITABLE HOSPITAL
        // =====================================================
        //
        // HospitalSelectionService also uses RoutingService.
        //
        // Therefore hospital selection uses the same selected
        // routing algorithm.
        //
        // =====================================================

        HospitalSelectionResponse hospitalResponse =
                hospitalSelectionService.findBestHospital(
                        emergencyId
                );


        // =====================================================
        // 6. LOAD SELECTED HOSPITAL
        // =====================================================

        Hospital hospital =
                hospitalRepository.findById(
                        hospitalResponse.hospitalId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Selected hospital not found"
                        )
                );


        // =====================================================
        // 7. FIND GRAPH NODE FOR AMBULANCE
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


        // =====================================================
        // 8. FIND GRAPH NODE FOR EMERGENCY
        // =====================================================

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


        // =====================================================
        // 9. AMBULANCE → EMERGENCY
        // =====================================================
        //
        // IMPORTANT:
        //
        // We use the selected algorithm explicitly here.
        //
        // This guarantees that the actual trip route uses
        // the same algorithm selected for this dispatch.
        //
        // =====================================================

        TrafficDijkstraResponse ambulanceToEmergency =
                routingService.findRoute(
                        ambulanceNode.id(),
                        emergencyNode.id(),
                        selectedAlgorithm
                );


        // =====================================================
        // 10. CREATE FIRST TRIP SEGMENT
        // =====================================================

        TripRoute.RouteSegment routeToEmergency =
                createRouteSegment(
                        ambulanceToEmergency
                );


        // =====================================================
        // 11. FIND GRAPH NODE FOR HOSPITAL
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


        // =====================================================
        // 12. EMERGENCY → HOSPITAL
        // =====================================================
        //
        // Use the exact same algorithm selected for this
        // dispatch.
        //
        // =====================================================

        TrafficDijkstraResponse emergencyToHospital =
                routingService.findRoute(
                        emergencyNode.id(),
                        hospitalNode.id(),
                        selectedAlgorithm
                );


        // =====================================================
        // 13. CREATE SECOND TRIP SEGMENT
        // =====================================================

        TripRoute.RouteSegment routeToHospital =
                createRouteSegment(
                        emergencyToHospital
                );


        // =====================================================
        // 14. CURRENT AMBULANCE → HOSPITAL ROUTE
        // =====================================================
        //
        // RouteService is still used because RouteDetails is
        // part of the existing frontend response.
        //
        // =====================================================

        var routeResponse =
                routeService.findRoute(
                        ambulance.getId(),
                        hospital.getId()
                );


        // =====================================================
        // 15. CHANGE AMBULANCE STATUS
        //
        // AVAILABLE → EN_ROUTE
        // =====================================================

        ambulance.setStatus(
                "EN_ROUTE"
        );

        ambulance =
                ambulanceRepository.save(
                        ambulance
                );


        // =====================================================
        // 16. CREATE PERSISTENT DISPATCH RECORD
        // =====================================================
        //
        // The actual selected algorithm is stored here.
        //
        // DIJKSTRA
        // or
        // ASTAR
        //
        // =====================================================

        Dispatch dispatch =
                Dispatch.builder()

                        .emergencyId(
                                emergency.getId()
                        )

                        .ambulanceId(
                                ambulance.getId()
                        )

                        .ambulanceNumber(
                                ambulance.getAmbulanceNumber()
                        )

                        .hospitalId(
                                hospital.getId()
                        )

                        .hospitalName(
                                hospital.getName()
                        )

                        .routingAlgorithm(
                                selectedAlgorithm.name()
                        )

                        .distanceToEmergencyKm(
                                routeToEmergency.distanceKm()
                        )

                        .timeToEmergencyMinutes(
                                routeToEmergency
                                        .estimatedTravelTimeMinutes()
                        )

                        .distanceToHospitalKm(
                                routeToHospital.distanceKm()
                        )

                        .timeToHospitalMinutes(
                                routeToHospital
                                        .estimatedTravelTimeMinutes()
                        )

                        .totalDistanceKm(
                                routeToEmergency.distanceKm()
                                        +
                                        routeToHospital.distanceKm()
                        )

                        .totalEstimatedTimeMinutes(
                                routeToEmergency
                                        .estimatedTravelTimeMinutes()
                                        +
                                        routeToHospital
                                                .estimatedTravelTimeMinutes()
                        )

                        .status(
                                "IN_PROGRESS"
                        )

                        .dispatchedAt(
                                LocalDateTime.now()
                        )

                        .build();


        Dispatch savedDispatch =
                dispatchRepository.save(
                        dispatch
                );


        // =====================================================
        // 17. INITIAL TRIP STATUS
        // =====================================================

        TripRoute trip =
                new TripRoute(
                        TripRoute.TripStatus.TO_EMERGENCY,
                        routeToEmergency,
                        routeToHospital
                );


        // =====================================================
        // 18. RETURN COMPLETE DISPATCH PLAN
        // =====================================================

        return new DispatchPlanResponse(

                savedDispatch.getId(),

                emergency.getId(),

                new DispatchPlanResponse.AmbulanceDetails(

                        ambulance.getId(),

                        ambulance.getAmbulanceNumber(),

                        ambulance.getType(),

                        ambulance.getStatus(),

                        ambulanceResponse.distanceKm(),

                        ambulanceResponse
                                .estimatedTravelTimeMinutes(),

                        ambulanceResponse.trafficLevel()
                ),

                new DispatchPlanResponse.HospitalDetails(

                        hospital.getId(),

                        hospital.getHospitalCode(),

                        hospital.getName(),

                        hospital.getFacilityType(),

                        hospital.getAvailableBeds(),

                        hospitalResponse.distanceKm(),

                        hospitalResponse
                                .estimatedTravelTimeMinutes(),

                        hospitalResponse.trafficLevel()
                ),

                new DispatchPlanResponse.RouteDetails(

                        routeResponse.sourceNode(),

                        routeResponse.destinationNode(),

                        routeResponse.distanceKm(),

                        routeResponse
                                .estimatedTravelTimeMinutes(),

                        routeResponse.trafficLevel(),

                        routeResponse.nodePath()
                ),

                trip
        );
    }


    // =========================================================
    // CREATE ROUTE SEGMENT
    // =========================================================

    private TripRoute.RouteSegment createRouteSegment(
            TrafficDijkstraResponse response
    ) {

        List<TripRoute.RoutePoint> coordinates =
                new ArrayList<>();


        // -----------------------------------------------------
        // Convert graph node IDs into coordinates.
        // -----------------------------------------------------

        for (String nodeId :
                response.path()) {

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

                response.estimatedTravelTimeMinutes(),

                response.trafficLevel(),

                response.path(),

                coordinates
        );
    }
}