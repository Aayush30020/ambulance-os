package com.ambulanceos.dto;

import java.util.List;

public record AlgorithmComparisonResponse(

        String sourceNode,

        String destinationNode,

        AlgorithmResult dijkstra,

        AlgorithmResult aStar,

        boolean sameOptimalDistance,

        boolean sameOptimalTravelTime,

        boolean samePath

) {

    public record AlgorithmResult(

            String algorithm,

            double distanceKm,

            double estimatedTravelTimeMinutes,

            String trafficLevel,

            int nodesExplored,

            long executionTimeMillis,

            List<String> path

    ) {
    }
}