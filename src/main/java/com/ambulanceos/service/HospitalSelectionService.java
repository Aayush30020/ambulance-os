package com.ambulanceos.service;

import com.ambulanceos.dto.DijkstraResponse;
import com.ambulanceos.dto.HospitalSelectionResponse;
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

    private final DijkstraService dijkstraService;


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
    // Map hospitals to graph nodes
    //     ↓
    // Dijkstra shortest road distance
    //     ↓
    // PriorityQueue
    //     ↓
    // Best hospital
    //
    // =========================================================

    public HospitalSelectionResponse findBestHospital(
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
        // 3. Get all hospitals
        // -----------------------------------------------------

        List<Hospital> hospitals =
                hospitalRepository.findAll();


        // -----------------------------------------------------
        // 4. Create PriorityQueue
        //
        // Primary priority:
        //     Shortest Dijkstra distance
        //
        // Secondary priority:
        //     More available beds
        //
        // -----------------------------------------------------

        PriorityQueue<HospitalDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator
                                .comparingDouble(
                                        HospitalDistance::distanceKm
                                )
                                .thenComparing(
                                        Comparator.comparingInt(
                                                HospitalDistance::availableBeds
                                        ).reversed()
                                )
                );


        // -----------------------------------------------------
        // 5. Check every hospital
        // -----------------------------------------------------

        for (Hospital hospital : hospitals) {

            // -------------------------------------------------
            // 5A. Hospital must have available beds
            // -------------------------------------------------

            if (hospital.getAvailableBeds() == null
                    || hospital.getAvailableBeds() <= 0) {

                continue;
            }


            // -------------------------------------------------
            // 5B. Check required facility
            // -------------------------------------------------

            if (!facilityMatches(
                    emergency.getFacility(),
                    hospital.getFacilityType()
            )) {

                continue;
            }


            // -------------------------------------------------
            // 5C. Find nearest graph node to hospital
            // -------------------------------------------------

            GraphNode hospitalNode =
                    findNearestGraphNode(
                            hospital.getLatitude(),
                            hospital.getLongitude()
                    );


            try {

                // -------------------------------------------------
                // 5D. Run Dijkstra
                //
                // Emergency → Hospital
                // -------------------------------------------------

                DijkstraResponse route =
                        dijkstraService.findShortestPath(

                                emergencyNode.id(),

                                hospitalNode.id()

                        );


                double distanceKm =
                        route.distanceKm();


                // -------------------------------------------------
                // 5E. Add hospital to PriorityQueue
                // -------------------------------------------------

                priorityQueue.offer(
                        new HospitalDistance(
                                hospital,
                                distanceKm
                        )
                );


            } catch (RuntimeException exception) {

                // -------------------------------------------------
                // If hospital cannot be reached through graph,
                // skip it and continue checking other hospitals.
                // -------------------------------------------------

                System.out.println(
                        "No route found for hospital "
                                + hospital.getHospitalCode()
                                + ": "
                                + exception.getMessage()
                );
            }
        }


        // -----------------------------------------------------
        // 6. Check whether a suitable hospital exists
        // -----------------------------------------------------

        if (priorityQueue.isEmpty()) {

            throw new RuntimeException(
                    "No reachable hospital found with required "
                            + "facility and available beds"
            );
        }


        // -----------------------------------------------------
        // 7. Select best hospital
        //
        // PriorityQueue gives us:
        //
        // shortest Dijkstra distance first
        //
        // -----------------------------------------------------

        HospitalDistance best =
                priorityQueue.poll();


        Hospital hospital =
                best.hospital();


        // -----------------------------------------------------
        // 8. Create response
        // -----------------------------------------------------

        return new HospitalSelectionResponse(

                emergency.getId(),

                hospital.getId(),

                hospital.getHospitalCode(),

                hospital.getName(),

                hospital.getFacilityType(),

                hospital.getAvailableBeds(),

                roundToTwoDecimals(
                        best.distanceKm()
                )

        );
    }


    // =========================================================
    // FACILITY MATCHING
    // =========================================================
    //
    // Example:
    //
    // Emergency = TRAUMA
    // Hospital  = TRAUMA
    //              ↓
    //             MATCH
    //
    // Emergency = ICU
    // Hospital  = ICU
    //              ↓
    //             MATCH
    //
    // GENERAL emergency can be handled by any hospital
    // with available beds.
    //
    // =========================================================

    private boolean facilityMatches(

            String requiredFacility,

            String hospitalFacility

    ) {

        if (requiredFacility == null
                || requiredFacility.isBlank()) {

            return true;
        }


        if ("GENERAL".equalsIgnoreCase(
                requiredFacility
        )) {

            return true;
        }


        if (hospitalFacility == null
                || hospitalFacility.isBlank()) {

            return false;
        }


        return requiredFacility.equalsIgnoreCase(
                hospitalFacility
        );
    }


    // =========================================================
    // FIND NEAREST GRAPH NODE
    // =========================================================
    //
    // Converts a hospital/emergency latitude and longitude
    // into the closest node in the Gurgaon road graph.
    //
    // Haversine is ONLY used for node matching.
    //
    // Actual route optimization is performed by Dijkstra.
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
    // HAVERSINE DISTANCE
    // =========================================================
    //
    // Used only to find the closest graph node.
    //
    // Dijkstra calculates the actual road distance.
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

    private record HospitalDistance(

            Hospital hospital,

            double distanceKm

    ) {

        private int availableBeds() {

            return hospital.getAvailableBeds();
        }
    }
}