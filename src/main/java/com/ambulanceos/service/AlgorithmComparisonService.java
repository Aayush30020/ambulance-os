package com.ambulanceos.service;

import com.ambulanceos.dto.AlgorithmComparisonResponse;
import com.ambulanceos.graph.GraphEdge;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlgorithmComparisonService {

    private final GurgaonRoadGraph roadGraph;

    private final TrafficService trafficService;

    private Double maximumGraphSpeedKmh;


    // =========================================================
    // MAIN COMPARISON
    // =========================================================

    public AlgorithmComparisonResponse compare(
            String sourceNode,
            String destinationNode
    ) {

        validateNode(sourceNode, "Source");

        validateNode(destinationNode, "Destination");


        // -----------------------------------------------------
        // Run Traffic-Aware Dijkstra
        // -----------------------------------------------------

        SearchResult dijkstraResult =
                runDijkstra(
                        sourceNode,
                        destinationNode
                );


        // -----------------------------------------------------
        // Run A*
        // -----------------------------------------------------

        SearchResult aStarResult =
                runAStar(
                        sourceNode,
                        destinationNode
                );


        // -----------------------------------------------------
        // Compare results
        // -----------------------------------------------------

        boolean sameDistance =
                Math.abs(
                        dijkstraResult.distanceKm()
                                - aStarResult.distanceKm()
                ) < 0.0001;


        boolean sameTravelTime =
                Math.abs(
                        dijkstraResult.travelTimeMinutes()
                                - aStarResult.travelTimeMinutes()
                ) < 0.0001;


        boolean samePath =
                dijkstraResult.path()
                        .equals(
                                aStarResult.path()
                        );


        return new AlgorithmComparisonResponse(

                sourceNode,

                destinationNode,

                new AlgorithmComparisonResponse.AlgorithmResult(

                        "TRAFFIC_DIJKSTRA",

                        roundToTwoDecimals(
                                dijkstraResult.distanceKm()
                        ),

                        roundToTwoDecimals(
                                dijkstraResult.travelTimeMinutes()
                        ),

                        dijkstraResult.trafficLevel(),

                        dijkstraResult.nodesExplored(),

                        dijkstraResult.executionTimeMillis(),

                        dijkstraResult.path()
                ),

                new AlgorithmComparisonResponse.AlgorithmResult(

                        "A_STAR",

                        roundToTwoDecimals(
                                aStarResult.distanceKm()
                        ),

                        roundToTwoDecimals(
                                aStarResult.travelTimeMinutes()
                        ),

                        aStarResult.trafficLevel(),

                        aStarResult.nodesExplored(),

                        aStarResult.executionTimeMillis(),

                        aStarResult.path()
                ),

                sameDistance,

                sameTravelTime,

                samePath
        );
    }


    // =========================================================
    // TRAFFIC-AWARE DIJKSTRA
    // =========================================================

    private SearchResult runDijkstra(
            String sourceNode,
            String destinationNode
    ) {

        long startTime =
                System.nanoTime();


        Map<String, Double> costs =
                new HashMap<>();


        Map<String, Double> distances =
                new HashMap<>();


        Map<String, String> previousNodes =
                new HashMap<>();


        PriorityQueue<NodeCost> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                NodeCost::cost
                        )
                );


        Set<String> processedNodes =
                new HashSet<>();


        costs.put(
                sourceNode,
                0.0
        );


        distances.put(
                sourceNode,
                0.0
        );


        priorityQueue.offer(
                new NodeCost(
                        sourceNode,
                        0.0
                )
        );


        while (!priorityQueue.isEmpty()) {

            NodeCost current =
                    priorityQueue.poll();


            String currentNode =
                    current.nodeId();


            double currentCost =
                    current.cost();


            double bestKnownCost =
                    costs.getOrDefault(
                            currentNode,
                            Double.POSITIVE_INFINITY
                    );


            if (currentCost > bestKnownCost) {
                continue;
            }


            if (processedNodes.contains(
                    currentNode
            )) {
                continue;
            }


            processedNodes.add(
                    currentNode
            );


            if (currentNode.equals(
                    destinationNode
            )) {
                break;
            }


            for (GraphEdge edge :
                    roadGraph.getNeighbors(
                            currentNode
                    )) {

                String neighbor =
                        edge.destinationNodeId();


                double roadDistance =
                        edge.distanceKm();


                double baseTravelTime =
                        edge.travelTimeMinutes();


                double trafficMultiplier =
                        trafficService.getTrafficMultiplier(
                                currentNode,
                                neighbor
                        );


                double trafficAdjustedTime =
                        baseTravelTime
                                * trafficMultiplier;


                double newCost =
                        currentCost
                                + trafficAdjustedTime;


                double currentDistance =
                        distances.getOrDefault(
                                currentNode,
                                Double.POSITIVE_INFINITY
                        );


                double newDistance =
                        currentDistance
                                + roadDistance;


                double knownCost =
                        costs.getOrDefault(
                                neighbor,
                                Double.POSITIVE_INFINITY
                        );


                if (newCost < knownCost) {

                    costs.put(
                            neighbor,
                            newCost
                    );


                    distances.put(
                            neighbor,
                            newDistance
                    );


                    previousNodes.put(
                            neighbor,
                            currentNode
                    );


                    priorityQueue.offer(
                            new NodeCost(
                                    neighbor,
                                    newCost
                            )
                    );
                }
            }
        }


        if (!costs.containsKey(
                destinationNode
        )) {

            throw new RuntimeException(
                    "No Traffic-Aware Dijkstra route exists between "
                            + sourceNode
                            + " and "
                            + destinationNode
            );
        }


        List<String> path =
                reconstructPath(
                        sourceNode,
                        destinationNode,
                        previousNodes
                );


        double travelTime =
                calculateTravelTime(
                        path
                );


        String trafficLevel =
                calculateTrafficLevel(
                        path
                );


        long executionTime =
                elapsedMilliseconds(
                        startTime
                );


        return new SearchResult(

                distances.get(
                        destinationNode
                ),

                travelTime,

                trafficLevel,

                processedNodes.size(),

                executionTime,

                path
        );
    }


    // =========================================================
    // A*
    // =========================================================

    private SearchResult runAStar(
            String sourceNode,
            String destinationNode
    ) {

        long startTime =
                System.nanoTime();


        Map<String, Double> gCosts =
                new HashMap<>();


        Map<String, Double> distances =
                new HashMap<>();


        Map<String, String> previousNodes =
                new HashMap<>();


        Set<String> processedNodes =
                new HashSet<>();


        PriorityQueue<NodeScore> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                NodeScore::fScore
                        )
                );


        gCosts.put(
                sourceNode,
                0.0
        );


        distances.put(
                sourceNode,
                0.0
        );


        double sourceHeuristic =
                heuristicMinutes(
                        sourceNode,
                        destinationNode
                );


        priorityQueue.offer(
                new NodeScore(
                        sourceNode,
                        0.0,
                        sourceHeuristic
                )
        );


        while (!priorityQueue.isEmpty()) {

            NodeScore current =
                    priorityQueue.poll();


            String currentNode =
                    current.nodeId();


            if (processedNodes.contains(
                    currentNode
            )) {
                continue;
            }


            processedNodes.add(
                    currentNode
            );


            if (currentNode.equals(
                    destinationNode
            )) {
                break;
            }


            for (GraphEdge edge :
                    roadGraph.getNeighbors(
                            currentNode
                    )) {

                String neighbor =
                        edge.destinationNodeId();


                double roadDistance =
                        edge.distanceKm();


                double baseTravelTime =
                        edge.travelTimeMinutes();


                double trafficMultiplier =
                        trafficService.getTrafficMultiplier(
                                currentNode,
                                neighbor
                        );


                double edgeCost =
                        baseTravelTime
                                * trafficMultiplier;


                double currentGCost =
                        gCosts.getOrDefault(
                                currentNode,
                                Double.POSITIVE_INFINITY
                        );


                double newGCost =
                        currentGCost
                                + edgeCost;


                double currentDistance =
                        distances.getOrDefault(
                                currentNode,
                                Double.POSITIVE_INFINITY
                        );


                double newDistance =
                        currentDistance
                                + roadDistance;


                double knownGCost =
                        gCosts.getOrDefault(
                                neighbor,
                                Double.POSITIVE_INFINITY
                        );


                if (newGCost < knownGCost) {

                    gCosts.put(
                            neighbor,
                            newGCost
                    );


                    distances.put(
                            neighbor,
                            newDistance
                    );


                    previousNodes.put(
                            neighbor,
                            currentNode
                    );


                    double heuristic =
                            heuristicMinutes(
                                    neighbor,
                                    destinationNode
                            );


                    double fScore =
                            newGCost
                                    + heuristic;


                    priorityQueue.offer(
                            new NodeScore(
                                    neighbor,
                                    newGCost,
                                    fScore
                            )
                    );
                }
            }
        }


        if (!gCosts.containsKey(
                destinationNode
        )) {

            throw new RuntimeException(
                    "No A* route exists between "
                            + sourceNode
                            + " and "
                            + destinationNode
            );
        }


        List<String> path =
                reconstructPath(
                        sourceNode,
                        destinationNode,
                        previousNodes
                );


        double travelTime =
                calculateTravelTime(
                        path
                );


        String trafficLevel =
                calculateTrafficLevel(
                        path
                );


        long executionTime =
                elapsedMilliseconds(
                        startTime
                );


        return new SearchResult(

                distances.get(
                        destinationNode
                ),

                travelTime,

                trafficLevel,

                processedNodes.size(),

                executionTime,

                path
        );
    }


    // =========================================================
    // A* HEURISTIC
    // =========================================================

    private double heuristicMinutes(
            String currentNodeId,
            String destinationNodeId
    ) {

        GraphNode current =
                roadGraph.getNode(
                        currentNodeId
                );


        GraphNode destination =
                roadGraph.getNode(
                        destinationNodeId
                );


        if (
                current == null
                        || destination == null
        ) {

            return 0.0;
        }


        double straightLineDistance =
                haversineDistance(
                        current.latitude(),
                        current.longitude(),
                        destination.latitude(),
                        destination.longitude()
                );


        double maximumSpeed =
                getMaximumGraphSpeedKmh();


        return (
                straightLineDistance
                        / maximumSpeed
        ) * 60.0;
    }


    // =========================================================
    // MAXIMUM GRAPH SPEED
    // =========================================================

    private double getMaximumGraphSpeedKmh() {

        if (maximumGraphSpeedKmh != null) {

            return maximumGraphSpeedKmh;
        }


        double maximumSpeed =
                0.0;


        for (GraphNode node :
                roadGraph.getNodes().values()) {

            List<GraphEdge> neighbors =
                    roadGraph.getNeighbors(
                            node.id()
                    );


            for (GraphEdge edge :
                    neighbors) {

                if (edge.travelTimeMinutes() <= 0) {
                    continue;
                }


                double speedKmh =
                        edge.distanceKm()
                                /
                                (
                                        edge.travelTimeMinutes()
                                                / 60.0
                                );


                maximumSpeed =
                        Math.max(
                                maximumSpeed,
                                speedKmh
                        );
            }
        }


        if (maximumSpeed <= 0.0) {

            throw new RuntimeException(
                    "Unable to determine maximum graph speed"
            );
        }


        maximumGraphSpeedKmh =
                maximumSpeed;


        System.out.println(
                "Benchmark A* maximum graph speed: "
                        + maximumGraphSpeedKmh
                        + " km/h"
        );


        return maximumGraphSpeedKmh;
    }


    // =========================================================
    // CALCULATE TRAVEL TIME
    // =========================================================

    private double calculateTravelTime(
            List<String> path
    ) {

        double totalTime =
                0.0;


        for (
                int i = 0;
                i < path.size() - 1;
                i++
        ) {

            String source =
                    path.get(i);


            String destination =
                    path.get(i + 1);


            GraphEdge edge =
                    findEdge(
                            source,
                            destination
                    );


            double multiplier =
                    trafficService.getTrafficMultiplier(
                            source,
                            destination
                    );


            totalTime +=
                    edge.travelTimeMinutes()
                            * multiplier;
        }


        return totalTime;
    }


    // =========================================================
    // CALCULATE TRAFFIC LEVEL
    // =========================================================

    private String calculateTrafficLevel(
            List<String> path
    ) {

        if (path.size() < 2) {

            return "LOW";
        }


        double totalMultiplier =
                0.0;


        int roadCount =
                path.size() - 1;


        for (
                int i = 0;
                i < path.size() - 1;
                i++
        ) {

            totalMultiplier +=
                    trafficService.getTrafficMultiplier(
                            path.get(i),
                            path.get(i + 1)
                    );
        }


        double averageMultiplier =
                totalMultiplier
                        / roadCount;


        return trafficService.getTrafficLevel(
                averageMultiplier
        );
    }


    // =========================================================
    // FIND EDGE
    // =========================================================

    private GraphEdge findEdge(
            String sourceNode,
            String destinationNode
    ) {

        for (GraphEdge edge :
                roadGraph.getNeighbors(
                        sourceNode
                )) {

            if (
                    edge.destinationNodeId()
                            .equals(
                                    destinationNode
                            )
            ) {

                return edge;
            }
        }


        throw new RuntimeException(
                "Road edge not found between "
                        + sourceNode
                        + " and "
                        + destinationNode
        );
    }


    // =========================================================
    // RECONSTRUCT PATH
    // =========================================================

    private List<String> reconstructPath(
            String sourceNode,
            String destinationNode,
            Map<String, String> previousNodes
    ) {

        List<String> path =
                new ArrayList<>();


        String current =
                destinationNode;


        while (current != null) {

            path.add(
                    current
            );


            if (current.equals(
                    sourceNode
            )) {

                break;
            }


            current =
                    previousNodes.get(
                            current
                    );
        }


        Collections.reverse(
                path
        );


        if (
                path.isEmpty()
                        || !path.get(0).equals(
                        sourceNode
                )
        ) {

            throw new RuntimeException(
                    "Unable to reconstruct route"
            );
        }


        return path;
    }


    // =========================================================
    // VALIDATE NODE
    // =========================================================

    private void validateNode(
            String nodeId,
            String label
    ) {

        if (roadGraph.getNode(nodeId) == null) {

            throw new RuntimeException(
                    label
                            + " node not found: "
                            + nodeId
            );
        }
    }


    // =========================================================
    // HAVERSINE
    // =========================================================

    private double haversineDistance(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {

        final double EARTH_RADIUS_KM =
                6371.0;


        double latitudeDifference =
                Math.toRadians(
                        latitude2 - latitude1
                );


        double longitudeDifference =
                Math.toRadians(
                        longitude2 - longitude1
                );


        double firstLatitude =
                Math.toRadians(
                        latitude1
                );


        double secondLatitude =
                Math.toRadians(
                        latitude2
                );


        double a =
                Math.sin(
                        latitudeDifference / 2
                )
                        *
                        Math.sin(
                                latitudeDifference / 2
                        )
                        +
                        Math.cos(
                                firstLatitude
                        )
                                *
                                Math.cos(
                                        secondLatitude
                                )
                                *
                                Math.sin(
                                        longitudeDifference / 2
                                )
                                *
                                Math.sin(
                                        longitudeDifference / 2
                                );


        double c =
                2
                        * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );


        return EARTH_RADIUS_KM * c;
    }


    // =========================================================
    // EXECUTION TIME
    // =========================================================

    private long elapsedMilliseconds(
            long startTime
    ) {

        return (
                System.nanoTime()
                        - startTime
        ) / 1_000_000;
    }


    // =========================================================
    // ROUNDING
    // =========================================================

    private double roundToTwoDecimals(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    // =========================================================
    // INTERNAL RESULT
    // =========================================================

    private record SearchResult(

            double distanceKm,

            double travelTimeMinutes,

            String trafficLevel,

            int nodesExplored,

            long executionTimeMillis,

            List<String> path

    ) {
    }


    // =========================================================
    // DIJKSTRA PRIORITY QUEUE NODE
    // =========================================================

    private record NodeCost(

            String nodeId,

            double cost

    ) {
    }


    // =========================================================
    // A* PRIORITY QUEUE NODE
    // =========================================================

    private record NodeScore(

            String nodeId,

            double gScore,

            double fScore

    ) {
    }
}