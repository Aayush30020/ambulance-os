package com.ambulanceos.dto;

public record AlgorithmBenchmarkResponse(

        String sourceNode,

        String destinationNode,

        AlgorithmResult dijkstra,

        AlgorithmResult aStar,

        ComparisonResult comparison

) {

    public record AlgorithmResult(

            String algorithm,

            double distanceKm,

            double travelTimeMinutes,

            String trafficLevel,

            int nodesExplored,

            long executionTimeMillis

    ) {
    }

    public record ComparisonResult(

            boolean sameOptimalDistance,

            boolean sameOptimalTravelTime,

            boolean samePath,

            double executionTimeImprovementPercent,

            int nodesExploredReduction

    ) {
    }
}