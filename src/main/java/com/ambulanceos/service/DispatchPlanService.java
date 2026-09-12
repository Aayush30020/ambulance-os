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
import com.ambulanceos.exception.AmbulanceNotFoundException;
import com.ambulanceos.exception.EmergencyNotFoundException;
import com.ambulanceos.exception.HospitalNotFoundException;
import com.ambulanceos.exception.RouteNotFoundException;
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
     * The selected algorithm is read from the routing settings
     * and then explicitly passed to the routing engine so that
     * every route in this dispatch uses the same algorithm.
     */
    private final RoutingService routingService;

    /*
     * Provides the currently selected routing algorithm.
     */
    private final RoutingSettingsService routingSettingsService;


    // =========================================================
    // CREATE COMPLETE DISPATCH PLAN
    // =========================================================

    @Transactional
    public DispatchPlanResponse createDispatchPlan(Long emergencyId) {

        // =====================================================
        // 1. FIND EMERGENCY
        // =====================================================

        Emergency emergency =
                emergencyRepository.findById(emergencyId)
                        .orElseThrow(() ->
                                new EmergencyNotFoundException(
                                        emergencyId
                                )
                        );


        // =====================================================
        // 2. GET CURRENT ROUTING ALGORITHM
        // =====================================================
        //
        // Read the algorithm once at the beginning of the
        // dispatch so the entire trip uses the same algorithm.
        //
        // =====================================================

        RoutingAlgorithm selectedAlgorithm =
                routingSettingsService.getRoutingAlgorithm();


        // =====================================================
        // 3. FIND BEST AVAILABLE AMBULANCE
        // =====================================================
        //
        // DispatchService performs ambulance selection using
        // the routing system.
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
                        new AmbulanceNotFoundException(
                                ambulanceResponse.ambulanceId()
                        )
                );


        // =====================================================
        // 5. FIND BEST SUITABLE HOSPITAL
        // =====================================================
        //
        // HospitalSelectionService filters hospitals by
        // suitability, available beds and route time.
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
                        new HospitalNotFoundException(
                                hospitalResponse.hospitalId()
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

            throw new RouteNotFoundException(
                    "Unable to find road node near ambulance "
                            + ambulance.getAmbulanceNumber()
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

            throw new RouteNotFoundException(
                    "Unable to find road node near emergency "
                            + emergency.getId()
            );
        }


        // =====================================================
        // 9. AMBULANCE → EMERGENCY
        // =====================================================
        //
        // Explicitly use the algorithm selected for this
        // dispatch.
        //
        // =====================================================

        TrafficDijkstraResponse ambulanceToEmergency;

        try {

            ambulanceToEmergency =
                    routingService.findRoute(
                            ambulanceNode.id(),
                            emergencyNode.id(),
                            selectedAlgorithm
                    );

        } catch (RuntimeException ex) {

            throw new RouteNotFoundException(
                    "Unable to calculate route from ambulance to emergency: "
                            + ex.getMessage()
            );
        }


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

            throw new RouteNotFoundException(
                    "Unable to find road node near hospital "
                            + hospital.getHospitalCode()
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

        TrafficDijkstraResponse emergencyToHospital;

        try {

            emergencyToHospital =
                    routingService.findRoute(
                            emergencyNode.id(),
                            hospitalNode.id(),
                            selectedAlgorithm
                    );

        } catch (RuntimeException ex) {

            throw new RouteNotFoundException(
                    "Unable to calculate route from emergency to hospital: "
                            + ex.getMessage()
            );
        }


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
        // RouteService is retained because RouteDetails is part
        // of the existing DispatchPlanResponse used by the
        // frontend.
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

        ambulance.setStatus("EN_ROUTE");

        ambulance =
                ambulanceRepository.save(
                        ambulance
                );


        // =====================================================
        // 16. CREATE PERSISTENT DISPATCH RECORD
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
                                        + routeToHospital.distanceKm()
                        )

                        .totalEstimatedTimeMinutes(
                                routeToEmergency
                                        .estimatedTravelTimeMinutes()
                                        + routeToHospital
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

        // -----------------------------------------------------
        // Defensive check for an invalid routing response.
        // -----------------------------------------------------

        if (response == null) {

            throw new RouteNotFoundException(
                    "Routing engine returned no route"
            );
        }

        if (response.path() == null ||
                response.path().isEmpty()) {

            throw new RouteNotFoundException(
                    "Routing engine returned an empty route"
            );
        }


        List<TripRoute.RoutePoint> coordinates =
                new ArrayList<>();


        // -----------------------------------------------------
        // Convert graph node IDs into coordinates.
        // -----------------------------------------------------

        for (String nodeId :
                response.path()) {

            GraphNode node =
                    roadGraph.getNode(nodeId);


            if (node == null) {

                throw new RouteNotFoundException(
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