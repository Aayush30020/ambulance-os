package com.ambulanceos.service;

import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.exception.RouteNotFoundException;
import com.ambulanceos.graph.GraphEdge;
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
public class TrafficAwareDijkstraService {

    private final GurgaonRoadGraph roadGraph;

    private final TrafficService trafficService;


    // =========================================================
    // TRAFFIC-AWARE DIJKSTRA
    // =========================================================
    //
    // Normal Dijkstra:
    //
    //     cost = base travel time
    //
    // Traffic-aware Dijkstra:
    //
    //     cost =
    //     base travel time × traffic multiplier
    //
    // Physical distance remains separate.
    //
    // Example:
    //
    // Road A:
    //     Base time = 2.0 min
    //     Traffic = 1.0
    //     Effective time = 2.0 min
    //
    // Road B:
    //     Base time = 1.5 min
    //     Traffic = 2.0
    //     Effective time = 3.0 min
    //
    // Dijkstra chooses Road A even though Road B
    // has the lower base travel time.
    //
    // =========================================================

    public TrafficDijkstraResponse findShortestPath(
            String sourceNode,
            String destinationNode
    ) {

        // =====================================================
        // 1. VALIDATE SOURCE
        // =====================================================

        if (roadGraph.getNode(sourceNode) == null) {

            throw new RouteNotFoundException(
                    "Source node not found: " + sourceNode
            );
        }


        // =====================================================
        // 2. VALIDATE DESTINATION
        // =====================================================

        if (roadGraph.getNode(destinationNode) == null) {

            throw new RouteNotFoundException(
                    "Destination node not found: " + destinationNode
            );
        }


        // =====================================================
        // 3. PHYSICAL DISTANCE MAP
        // =====================================================
        //
        // Stores actual road distance from source.
        //
        // This is NOT used as the Dijkstra optimization cost.
        //
        // =====================================================

        Map<String, Double> distances =
                new HashMap<>();


        // =====================================================
        // 4. TRAFFIC-ADJUSTED COST MAP
        // =====================================================
        //
        // Stores the total estimated travel time used
        // by Dijkstra.
        //
        // =====================================================

        Map<String, Double> costs =
                new HashMap<>();


        // =====================================================
        // 5. PREVIOUS NODE MAP
        // =====================================================

        Map<String, String> previousNodes =
                new HashMap<>();


        // =====================================================
        // 6. INITIALIZE SOURCE
        // =====================================================

        distances.put(
                sourceNode,
                0.0
        );

        costs.put(
                sourceNode,
                0.0
        );


        // =====================================================
        // 7. PRIORITY QUEUE
        // =====================================================
        //
        // Lowest traffic-adjusted travel time gets priority.
        //
        // =====================================================

        PriorityQueue<NodeCost> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                NodeCost::cost
                        )
                );


        priorityQueue.offer(
                new NodeCost(
                        sourceNode,
                        0.0
                )
        );


        // =====================================================
        // 8. PROCESSED NODES
        // =====================================================

        Set<String> processedNodes =
                new HashSet<>();


        // =====================================================
        // 9. MAIN DIJKSTRA LOOP
        // =====================================================

        while (!priorityQueue.isEmpty()) {

            NodeCost current =
                    priorityQueue.poll();


            String currentNode =
                    current.nodeId();


            double currentCost =
                    current.cost();


            // -------------------------------------------------
            // Skip nodes already finalized.
            // -------------------------------------------------

            if (processedNodes.contains(
                    currentNode
            )) {

                continue;
            }


            // -------------------------------------------------
            // Mark node as finalized.
            // -------------------------------------------------

            processedNodes.add(
                    currentNode
            );


            // -------------------------------------------------
            // Destination reached.
            // -------------------------------------------------

            if (currentNode.equals(
                    destinationNode
            )) {

                break;
            }


            // =================================================
            // 10. RELAX NEIGHBORING EDGES
            // =================================================

            List<GraphEdge> neighbors =
                    roadGraph.getNeighbors(
                            currentNode
                    );


            for (GraphEdge edge :
                    neighbors) {

                String neighbor =
                        edge.destinationNodeId();


                // -------------------------------------------------
                // Physical road distance.
                // -------------------------------------------------

                double roadDistance =
                        edge.distanceKm();


                // -------------------------------------------------
                // Base travel time from the OSM graph.
                // -------------------------------------------------

                double baseTravelTime =
                        edge.travelTimeMinutes();


                // -------------------------------------------------
                // Get simulated traffic multiplier.
                // -------------------------------------------------

                double trafficMultiplier =
                        trafficService.getTrafficMultiplier(
                                currentNode,
                                neighbor
                        );


                // -------------------------------------------------
                // Traffic-adjusted travel time.
                //
                // THIS is the Dijkstra edge cost.
                // -------------------------------------------------

                double trafficAdjustedTime =
                        baseTravelTime
                                * trafficMultiplier;


                // -------------------------------------------------
                // Total cost from source.
                // -------------------------------------------------

                double newCost =
                        currentCost
                                + trafficAdjustedTime;


                // -------------------------------------------------
                // Total physical distance.
                //
                // This remains independent from traffic.
                // -------------------------------------------------

                double currentDistance =
                        distances.getOrDefault(
                                currentNode,
                                Double.POSITIVE_INFINITY
                        );


                double newDistance =
                        currentDistance
                                + roadDistance;


                // =================================================
                // 11. RELAXATION
                // =================================================

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


        // =====================================================
        // 12. CHECK WHETHER DESTINATION WAS REACHED
        // =====================================================

        double destinationCost =
                costs.getOrDefault(
                        destinationNode,
                        Double.POSITIVE_INFINITY
                );


        if (Double.isInfinite(
                destinationCost
        )) {

            throw new RouteNotFoundException(
                    "No traffic-aware route exists between "
                            + sourceNode
                            + " and "
                            + destinationNode
            );
        }


        // =====================================================
        // 13. RECONSTRUCT PATH
        // =====================================================

        List<String> path =
                reconstructPath(
                        sourceNode,
                        destinationNode,
                        previousNodes
                );


        // =====================================================
        // 14. CALCULATE FINAL TRAVEL TIME
        // =====================================================

        double estimatedTravelTimeMinutes =
                calculateTotalTravelTime(
                        path
                );


        // =====================================================
        // 15. CALCULATE OVERALL TRAFFIC LEVEL
        // =====================================================

        String overallTrafficLevel =
                calculateOverallTrafficLevel(
                        path
                );


        // =====================================================
        // 16. RETURN RESPONSE
        // =====================================================

        return new TrafficDijkstraResponse(

                sourceNode,

                destinationNode,

                roundToTwoDecimals(
                        distances.get(
                                destinationNode
                        )
                ),

                roundToTwoDecimals(
                        estimatedTravelTimeMinutes
                ),

                overallTrafficLevel,

                path
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

            throw new RouteNotFoundException(
                    "Unable to reconstruct traffic-aware route"
            );
        }


        return path;
    }


    // =========================================================
    // CALCULATE TOTAL TRAVEL TIME
    // =========================================================
    //
    // For every selected edge:
    //
    //     base travel time
    //             ×
    //     traffic multiplier
    //
    // =========================================================

    private double calculateTotalTravelTime(
            List<String> path
    ) {

        double totalTimeMinutes =
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


            double trafficMultiplier =
                    trafficService.getTrafficMultiplier(
                            source,
                            destination
                    );


            totalTimeMinutes +=
                    edge.travelTimeMinutes()
                            * trafficMultiplier;
        }


        return totalTimeMinutes;
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
                            .equals(destinationNode)
            ) {

                return edge;
            }
        }


        throw new RouteNotFoundException(
                "Road edge not found between "
                        + sourceNode
                        + " and "
                        + destinationNode
        );
    }


    // =========================================================
    // CALCULATE OVERALL TRAFFIC LEVEL
    // =========================================================

    private String calculateOverallTrafficLevel(
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
    // ROUND VALUES
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

    private record NodeCost(
            String nodeId,
            double cost
    ) {
    }
}