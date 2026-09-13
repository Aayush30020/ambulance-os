package com.ambulanceos.service;

import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.exception.EmergencyNotFoundException;
import com.ambulanceos.exception.NoSuitableHospitalException;
import com.ambulanceos.exception.RouteNotFoundException;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.EmergencyRepository;
import com.ambulanceos.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalSelectionService {

    private final EmergencyRepository emergencyRepository;

    private final HospitalRepository hospitalRepository;

    private final GurgaonRoadGraph roadGraph;

    /*
     * Central routing gateway.
     *
     * RoutingService supports:
     *
     *      Traffic-Aware Dijkstra
     *              OR
     *             A*
     */
    private final RoutingService routingService;

    /*
     * Provides the currently configured routing algorithm.
     *
     * The one-argument method uses the currently configured
     * algorithm.
     *
     * DispatchPlanService can explicitly provide an algorithm
     * through the two-argument method so the entire dispatch
     * uses one consistent algorithm.
     */
    private final RoutingSettingsService routingSettingsService;


    // =========================================================
    // FIND BEST HOSPITAL
    // =========================================================

    public HospitalSelectionResponse findBestHospital(
            Long emergencyId
    ) {

        RoutingAlgorithm selectedAlgorithm =
                routingSettingsService.getRoutingAlgorithm();

        return findBestHospital(
                emergencyId,
                selectedAlgorithm
        );
    }


    // =========================================================
    // FIND BEST HOSPITAL WITH EXPLICIT ALGORITHM
    // =========================================================

    public HospitalSelectionResponse findBestHospital(
            Long emergencyId,
            RoutingAlgorithm selectedAlgorithm
    ) {

        // =====================================================
        // 1. VALIDATE INPUT
        // =====================================================

        if (emergencyId == null) {
            throw new IllegalArgumentException(
                    "Emergency ID must not be null"
            );
        }

        if (selectedAlgorithm == null) {
            throw new IllegalArgumentException(
                    "Routing algorithm must not be null"
            );
        }


        // =====================================================
        // 2. FIND EMERGENCY
        // =====================================================

        Emergency emergency =
                emergencyRepository
                        .findById(emergencyId)
                        .orElseThrow(() ->
                                new EmergencyNotFoundException(
                                        emergencyId
                                )
                        );


        // =====================================================
        // 3. VALIDATE EMERGENCY COORDINATES
        // =====================================================

        if (!isValidCoordinate(
                emergency.getLatitude(),
                emergency.getLongitude()
        )) {

            throw new NoSuitableHospitalException(
                    "Emergency has invalid geographic coordinates"
            );
        }


        // =====================================================
        // 4. FIND EMERGENCY GRAPH NODE
        // =====================================================

        GraphNode emergencyNode =
                roadGraph.findNearestNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                );

        if (emergencyNode == null) {

            throw new NoSuitableHospitalException(
                    "Unable to find a road node near the emergency"
            );
        }


        // =====================================================
        // 5. GET ALL HOSPITALS
        // =====================================================

        List<Hospital> hospitals =
                hospitalRepository.findAll();


        if (hospitals == null || hospitals.isEmpty()) {

            throw new NoSuitableHospitalException(
                    "No hospitals are available"
            );
        }


        // =====================================================
        // 6. PRIORITY QUEUE
        // =====================================================
        //
        // Primary:
        //     Lowest traffic-adjusted travel time
        //
        // Secondary:
        //     More available beds
        //
        // =====================================================

        PriorityQueue<HospitalDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator
                                .comparingDouble(
                                        HospitalDistance
                                                ::travelTimeMinutes
                                )
                                .thenComparing(
                                        Comparator.comparingInt(
                                                HospitalDistance
                                                        ::availableBeds
                                        ).reversed()
                                )
                );


        // =====================================================
        // 7. EVALUATE EVERY HOSPITAL
        // =====================================================

        for (Hospital hospital : hospitals) {

            if (hospital == null) {
                continue;
            }


            // -------------------------------------------------
            // 7A. AVAILABLE BEDS
            // -------------------------------------------------

            Integer availableBeds =
                    hospital.getAvailableBeds();

            if (
                    availableBeds == null
                            || availableBeds <= 0
            ) {

                continue;
            }


            // -------------------------------------------------
            // 7B. FACILITY MATCH
            // -------------------------------------------------

            if (
                    !facilityMatches(
                            emergency.getFacility(),
                            hospital.getFacilityType()
                    )
            ) {

                continue;
            }


            // -------------------------------------------------
            // 7C. HOSPITAL COORDINATES
            // -------------------------------------------------

            if (!isValidCoordinate(
                    hospital.getLatitude(),
                    hospital.getLongitude()
            )) {

                log.warn(
                        "Skipping hospital {} because it has "
                                + "invalid coordinates",
                        hospital.getHospitalCode()
                );

                continue;
            }


            // -------------------------------------------------
            // 7D. FIND HOSPITAL GRAPH NODE
            // -------------------------------------------------

            GraphNode hospitalNode =
                    roadGraph.findNearestNode(
                            hospital.getLatitude(),
                            hospital.getLongitude()
                    );

            if (hospitalNode == null) {

                log.warn(
                        "Unable to find road node for hospital {}",
                        hospital.getHospitalCode()
                );

                continue;
            }


            // =================================================
            // 7E. ROUTE EMERGENCY → HOSPITAL
            // =================================================

            try {

                TrafficDijkstraResponse route =
                        routingService.findRoute(
                                emergencyNode.id(),
                                hospitalNode.id(),
                                selectedAlgorithm
                        );

                if (route == null) {

                    log.warn(
                            "Routing service returned null route "
                                    + "for hospital {}",
                            hospital.getHospitalCode()
                    );

                    continue;
                }


                // ---------------------------------------------
                // Defensive route validation
                // ---------------------------------------------

                if (
                        !Double.isFinite(
                                route.distanceKm()
                        )
                                || route.distanceKm() < 0
                ) {

                    log.warn(
                            "Skipping hospital {} because route "
                                    + "distance is invalid",
                            hospital.getHospitalCode()
                    );

                    continue;
                }

                if (
                        !Double.isFinite(
                                route.estimatedTravelTimeMinutes()
                        )
                                || route.estimatedTravelTimeMinutes() < 0
                ) {

                    log.warn(
                            "Skipping hospital {} because route "
                                    + "travel time is invalid",
                            hospital.getHospitalCode()
                    );

                    continue;
                }


                // ---------------------------------------------
                // Add candidate to priority queue
                // ---------------------------------------------

                priorityQueue.offer(
                        new HospitalDistance(
                                hospital,
                                route.distanceKm(),
                                route.estimatedTravelTimeMinutes(),
                                route.trafficLevel()
                        )
                );

            } catch (RouteNotFoundException exception) {

                log.warn(
                        "No route found for hospital {}: {}",
                        hospital.getHospitalCode(),
                        exception.getMessage()
                );
            }
        }


        // =====================================================
        // 8. NO SUITABLE HOSPITAL
        // =====================================================

        if (priorityQueue.isEmpty()) {

            throw new NoSuitableHospitalException(
                    "No reachable hospital found with required "
                            + "facility and available beds"
            );
        }


        // =====================================================
        // 9. SELECT BEST HOSPITAL
        // =====================================================

        HospitalDistance best =
                priorityQueue.poll();

        Hospital hospital =
                best.hospital();


        // =====================================================
        // 10. LOG SELECTION
        // =====================================================

        log.info(
                "Hospital selected: code={}, name={}, facility={}, "
                        + "availableBeds={}, distanceKm={}, "
                        + "estimatedTimeMinutes={}, trafficLevel={}, "
                        + "routingAlgorithm={}",
                hospital.getHospitalCode(),
                hospital.getName(),
                hospital.getFacilityType(),
                hospital.getAvailableBeds(),
                roundToTwoDecimals(
                        best.distanceKm()
                ),
                roundToTwoDecimals(
                        best.travelTimeMinutes()
                ),
                best.trafficLevel(),
                selectedAlgorithm
        );


        // =====================================================
        // 11. RETURN RESPONSE
        // =====================================================

        return new HospitalSelectionResponse(
                emergency.getId(),
                hospital.getId(),
                hospital.getHospitalCode(),
                hospital.getName(),
                hospital.getFacilityType(),
                hospital.getAvailableBeds(),
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
    // FACILITY MATCHING
    // =========================================================

    private boolean facilityMatches(
            String requiredFacility,
            String hospitalFacility
    ) {

        if (
                requiredFacility == null
                        || requiredFacility.isBlank()
        ) {

            return true;
        }


        if (
                "GENERAL".equalsIgnoreCase(
                        requiredFacility.trim()
                )
        ) {

            return true;
        }


        if (
                hospitalFacility == null
                        || hospitalFacility.isBlank()
        ) {

            return false;
        }


        return requiredFacility
                .trim()
                .equalsIgnoreCase(
                        hospitalFacility.trim()
                );
    }


    // =========================================================
    // COORDINATE VALIDATION
    // =========================================================

    private boolean isValidCoordinate(
            double latitude,
            double longitude
    ) {

        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
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

    private record HospitalDistance(

            Hospital hospital,

            double distanceKm,

            double travelTimeMinutes,

            String trafficLevel

    ) {

        private int availableBeds() {

            return hospital.getAvailableBeds();
        }
    }
}