package com.ambulanceos.service;

import com.ambulanceos.graph.GraphNode;
import org.springframework.stereotype.Service;

@Service
public class TrafficService {

    /*
     * Returns a simulated traffic multiplier for a road segment.
     *
     * 1.0 = Normal traffic
     * 1.25 = Moderate traffic
     * 1.50 = Heavy traffic
     * 2.00 = Severe traffic
     *
     * The multiplier increases the effective travel cost
     * used by Dijkstra.
     *
     * The actual road distance does NOT change.
     */

    public double getTrafficMultiplier(
            String sourceNodeId,
            String destinationNodeId
    ) {

        /*
         * Create a deterministic value from the two node IDs.
         *
         * Sorting the IDs ensures that:
         *
         * A → B
         *
         * and
         *
         * B → A
         *
         * receive the same traffic level.
         */

        String first =
                sourceNodeId.compareTo(destinationNodeId) < 0
                        ? sourceNodeId
                        : destinationNodeId;

        String second =
                sourceNodeId.compareTo(destinationNodeId) < 0
                        ? destinationNodeId
                        : sourceNodeId;


        String roadKey =
                first + "-" + second;


        int hash =
                Math.abs(roadKey.hashCode());


        /*
         * Convert the hash into one of four
         * simulated traffic levels.
         */

        int trafficLevel =
                hash % 4;


        return switch (trafficLevel) {

            case 0 -> 1.00;   // LOW

            case 1 -> 1.25;   // MODERATE

            case 2 -> 1.50;   // HEAVY

            default -> 2.00;  // SEVERE
        };
    }


    /*
     * Convert traffic multiplier into a readable label.
     */

    public String getTrafficLevel(
            double multiplier
    ) {

        if (multiplier <= 1.00) {
            return "LOW";
        }

        if (multiplier <= 1.25) {
            return "MODERATE";
        }

        if (multiplier <= 1.50) {
            return "HEAVY";
        }

        return "SEVERE";
    }


    /*
     * Estimate travel time for a road segment.
     *
     * Base emergency vehicle speed:
     * 40 km/h
     *
     * Traffic multiplier increases travel time.
     */

    public double calculateTravelTimeMinutes(

            double distanceKm,

            double trafficMultiplier

    ) {

        final double BASE_SPEED_KMH = 40.0;


        double baseTimeHours =
                distanceKm / BASE_SPEED_KMH;


        double trafficAdjustedTimeHours =
                baseTimeHours * trafficMultiplier;


        return trafficAdjustedTimeHours * 60.0;
    }
}