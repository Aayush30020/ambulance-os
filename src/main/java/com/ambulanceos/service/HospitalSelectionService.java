package com.ambulanceos.service;

import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.EmergencyRepository;
import com.ambulanceos.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class HospitalSelectionService {

    private final EmergencyRepository emergencyRepository;

    private final HospitalRepository hospitalRepository;

    private final GurgaonRoadGraph roadGraph;

    /*
     * Central routing service.
     *
     * This automatically uses whichever routing algorithm
     * is currently selected in Settings.
     */
    private final RoutingService routingService;


    // =========================================================
    // FIND BEST HOSPITAL
    // =========================================================
    //
    // Selection process:
    //
    // Emergency
    //     ↓
    // Required facility
    //     ↓
    // Hospitals with available beds
    //     ↓
    // Nearest OSM node
    //     ↓
    // RoutingService
    //     ↓
    // Dijkstra OR A*
    //     ↓
    // PriorityQueue
    //     ↓
    // Fastest suitable hospital
    //
    // Primary:
    //     Lowest traffic-adjusted travel time
    //
    // Secondary:
    //     More available beds
    //
    // =========================================================

    public HospitalSelectionResponse findBestHospital(
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
        // 2. FIND EMERGENCY GRAPH NODE
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
        // 3. GET ALL HOSPITALS
        // =====================================================

        List<Hospital> hospitals =
                hospitalRepository.findAll();


        // =====================================================
        // 4. CREATE PRIORITY QUEUE
        // =====================================================
        //
        // Primary:
        //     Lowest travel time
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
        // 5. CHECK EVERY HOSPITAL
        // =====================================================

        for (Hospital hospital :
                hospitals) {

            // -------------------------------------------------
            // 5A. Hospital must have available beds.
            // -------------------------------------------------

            if (
                    hospital.getAvailableBeds() == null
                            || hospital.getAvailableBeds() <= 0
            ) {

                continue;
            }


            // -------------------------------------------------
            // 5B. Hospital must support required facility.
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
            // 5C. Find nearest OSM node to hospital.
            // -------------------------------------------------

            GraphNode hospitalNode =
                    roadGraph.findNearestNode(
                            hospital.getLatitude(),
                            hospital.getLongitude()
                    );


            if (hospitalNode == null) {

                System.out.println(
                        "Unable to find road node for hospital "
                                + hospital.getHospitalCode()
                );

                continue;
            }


            try {

                // =================================================
                // 5D. ROUTE EMERGENCY → HOSPITAL
                // =================================================
                //
                // RoutingService reads the current algorithm
                // from PostgreSQL.
                //
                // DIJKSTRA:
                //     Traffic-aware Dijkstra
                //
                // ASTAR:
                //     A*
                //
                // =================================================

                TrafficDijkstraResponse route =
                        routingService.findRoute(
                                emergencyNode.id(),
                                hospitalNode.id()
                        );


                // -------------------------------------------------
                // 5E. ADD HOSPITAL TO PRIORITY QUEUE
                // -------------------------------------------------

                priorityQueue.offer(
                        new HospitalDistance(
                                hospital,
                                route.distanceKm(),
                                route.estimatedTravelTimeMinutes(),
                                route.trafficLevel()
                        )
                );


            } catch (RuntimeException exception) {

                // -------------------------------------------------
                // Skip unreachable hospitals.
                // -------------------------------------------------

                System.out.println(
                        "No route found for hospital "
                                + hospital.getHospitalCode()
                                + ": "
                                + exception.getMessage()
                );
            }
        }


        // =====================================================
        // 6. CHECK WHETHER SUITABLE HOSPITAL EXISTS
        // =====================================================

        if (priorityQueue.isEmpty()) {

            throw new RuntimeException(
                    "No reachable hospital found with required "
                            + "facility and available beds"
            );
        }


        // =====================================================
        // 7. SELECT BEST HOSPITAL
        // =====================================================

        HospitalDistance best =
                priorityQueue.poll();


        Hospital hospital =
                best.hospital();


        // =====================================================
        // 8. PRINT SELECTION DETAILS
        // =====================================================

        System.out.println();

        System.out.println(
                "=========================================="
        );

        System.out.println(
                " Traffic-Aware Hospital Selection"
        );

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Hospital: "
                        + hospital.getName()
        );

        System.out.println(
                "Hospital Code: "
                        + hospital.getHospitalCode()
        );

        System.out.println(
                "Facility: "
                        + hospital.getFacilityType()
        );

        System.out.println(
                "Available Beds: "
                        + hospital.getAvailableBeds()
        );

        System.out.println(
                "Road Distance: "
                        + roundToTwoDecimals(
                        best.distanceKm()
                )
                        + " km"
        );

        System.out.println(
                "Estimated Travel Time: "
                        + roundToTwoDecimals(
                        best.travelTimeMinutes()
                )
                        + " min"
        );

        System.out.println(
                "Traffic Level: "
                        + best.trafficLevel()
        );

        System.out.println(
                "Selection: Fastest suitable hospital"
        );

        System.out.println(
                "=========================================="
        );

        System.out.println();


        // =====================================================
        // 9. RETURN RESPONSE
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

        // -----------------------------------------------------
        // No specific facility requirement.
        // -----------------------------------------------------

        if (
                requiredFacility == null
                        || requiredFacility.isBlank()
        ) {

            return true;
        }


        // -----------------------------------------------------
        // GENERAL emergency can use any hospital.
        // -----------------------------------------------------

        if (
                "GENERAL".equalsIgnoreCase(
                        requiredFacility
                )
        ) {

            return true;
        }


        // -----------------------------------------------------
        // Hospital must have a facility type.
        // -----------------------------------------------------

        if (
                hospitalFacility == null
                        || hospitalFacility.isBlank()
        ) {

            return false;
        }


        // -----------------------------------------------------
        // Case-insensitive comparison.
        // -----------------------------------------------------

        return requiredFacility.equalsIgnoreCase(
                hospitalFacility
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