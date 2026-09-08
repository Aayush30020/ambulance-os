package com.ambulanceos.service;

import com.ambulanceos.dto.TrafficDijkstraResponse;
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
public class AStarService {

    private final GurgaonRoadGraph roadGraph;

    private final TrafficService trafficService;

    /*
     * Cached maximum speed of the road graph.
     *
     * We calculate this only once because scanning all
     * ~332,000 edges every time A* calculates a heuristic
     * would be inefficient.
     */
    private Double maximumGraphSpeedKmh;


    // =========================================================
    // A* SHORTEST PATH
    // =========================================================
    //
    // A* uses:
    //
    //      f(n) = g(n) + h(n)
    //
    // g(n) = actual traffic-adjusted travel time from
    //        the source to the current node.
    //
    // h(n) = estimated minimum remaining travel time from
    //        the current node to the destination.
    //
    // Unlike Dijkstra, A* uses h(n) to guide the search
    // toward the destination.
    //
    // =========================================================

    public TrafficDijkstraResponse findShortestPath(
            String sourceNode,
            String destinationNode
    ) {

        // =====================================================
        // 1. VALIDATE SOURCE NODE
        // =====================================================

        if (roadGraph.getNode(sourceNode) == null) {

            throw new RuntimeException(
                    "Source node not found: "
                            + sourceNode
            );
        }


        // =====================================================
        // 2. VALIDATE DESTINATION NODE
        // =====================================================

        if (roadGraph.getNode(destinationNode) == null) {

            throw new RuntimeException(
                    "Destination node not found: "
                            + destinationNode
            );
        }


        // =====================================================
        // 3. STORE PHYSICAL DISTANCE
        // =====================================================
        //
        // distances[node] = physical road distance from
        // source to that node.
        //
        // This is separate from the A* cost because the
        // algorithm is optimizing travel time, not distance.
        //
        // =====================================================

        Map<String, Double> distances =
                new HashMap<>();


        // =====================================================
        // 4. STORE G-COST
        // =====================================================
        //
        // gCosts[node] = actual traffic-adjusted travel time
        // from source to that node.
        //
        // =====================================================

        Map<String, Double> gCosts =
                new HashMap<>();


        // =====================================================
        // 5. STORE PREVIOUS NODES
        // =====================================================
        //
        // Used later to reconstruct the final route.
        //
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

        gCosts.put(
                sourceNode,
                0.0
        );


        // =====================================================
        // 7. PRIORITY QUEUE
        // =====================================================
        //
        // A* chooses the node with the smallest:
        //
        //      f(n) = g(n) + h(n)
        //
        // =====================================================

        PriorityQueue<NodeScore> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                NodeScore::fScore
                        )
                );


        // Calculate heuristic for the source node.

        double sourceHeuristic =
                heuristicMinutes(
                        sourceNode,
                        destinationNode
                );


        // Add source to priority queue.

        priorityQueue.offer(
                new NodeScore(
                        sourceNode,
                        0.0,
                        sourceHeuristic
                )
        );


        // =====================================================
        // 8. PROCESSED NODES
        // =====================================================
        //
        // Once a node has been finalized, we don't process
        // it again.
        //
        // =====================================================

        Set<String> processedNodes =
                new HashSet<>();


        // =====================================================
        // 9. MAIN A* LOOP
        // =====================================================

        while (!priorityQueue.isEmpty()) {

            NodeScore current =
                    priorityQueue.poll();


            String currentNode =
                    current.nodeId();


            // -------------------------------------------------
            // Ignore nodes that were already finalized.
            // -------------------------------------------------

            if (processedNodes.contains(
                    currentNode
            )) {

                continue;
            }


            // -------------------------------------------------
            // Mark current node as finalized.
            // -------------------------------------------------

            processedNodes.add(
                    currentNode
            );


            // -------------------------------------------------
            // If destination is reached, stop.
            // -------------------------------------------------

            if (currentNode.equals(
                    destinationNode
            )) {

                break;
            }


            // =================================================
            // 10. GET CURRENT NODE'S NEIGHBORS
            // =================================================

            List<GraphEdge> neighbors =
                    roadGraph.getNeighbors(
                            currentNode
                    );


            // =================================================
            // 11. EXPLORE EACH NEIGHBOR
            // =================================================

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
                // Base travel time without traffic.
                // -------------------------------------------------

                double baseTravelTime =
                        edge.travelTimeMinutes();


                // -------------------------------------------------
                // Get deterministic traffic multiplier.
                //
                // LOW       = 1.00
                // MODERATE  = 1.25
                // HEAVY     = 1.50
                // SEVERE    = 2.00
                // -------------------------------------------------

                double trafficMultiplier =
                        trafficService.getTrafficMultiplier(
                                currentNode,
                                neighbor
                        );


                // -------------------------------------------------
                // Traffic-adjusted travel time for this edge.
                //
                // This is the actual edge cost used by A*.
                // -------------------------------------------------

                double edgeCost =
                        baseTravelTime
                                * trafficMultiplier;


                // =================================================
                // 12. CALCULATE NEW G COST
                // =================================================
                //
                // g(newNode) =
                //      g(currentNode) + edge cost
                //
                // =================================================

                double currentGCost =
                        gCosts.getOrDefault(
                                currentNode,
                                Double.POSITIVE_INFINITY
                        );


                double newGCost =
                        currentGCost
                                + edgeCost;


                // =================================================
                // 13. CALCULATE NEW PHYSICAL DISTANCE
                // =================================================

                double currentDistance =
                        distances.getOrDefault(
                                currentNode,
                                Double.POSITIVE_INFINITY
                        );


                double newDistance =
                        currentDistance
                                + roadDistance;


                // =================================================
                // 14. CHECK WHETHER THIS PATH IS BETTER
                // =================================================

                double knownGCost =
                        gCosts.getOrDefault(
                                neighbor,
                                Double.POSITIVE_INFINITY
                        );


                if (newGCost < knownGCost) {

                    // -------------------------------------------------
                    // Save better travel-time cost.
                    // -------------------------------------------------

                    gCosts.put(
                            neighbor,
                            newGCost
                    );


                    // -------------------------------------------------
                    // Save physical distance.
                    // -------------------------------------------------

                    distances.put(
                            neighbor,
                            newDistance
                    );


                    // -------------------------------------------------
                    // Remember how we reached this node.
                    // -------------------------------------------------

                    previousNodes.put(
                            neighbor,
                            currentNode
                    );


                    // =================================================
                    // 15. CALCULATE HEURISTIC
                    // =================================================
                    //
                    // h(n) = estimated remaining travel time.
                    //
                    // =================================================

                    double heuristic =
                            heuristicMinutes(
                                    neighbor,
                                    destinationNode
                            );


                    // =================================================
                    // 16. CALCULATE F SCORE
                    // =================================================
                    //
                    // f(n) = g(n) + h(n)
                    //
                    // =================================================

                    double fScore =
                            newGCost
                                    + heuristic;


                    // -------------------------------------------------
                    // Add node to priority queue.
                    // -------------------------------------------------

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


        // =====================================================
        // 17. CHECK WHETHER DESTINATION WAS REACHED
        // =====================================================

        double destinationCost =
                gCosts.getOrDefault(
                        destinationNode,
                        Double.POSITIVE_INFINITY
                );


        if (Double.isInfinite(
                destinationCost
        )) {

            throw new RuntimeException(
                    "No A* route exists between "
                            + sourceNode
                            + " and "
                            + destinationNode
            );
        }


        // =====================================================
        // 18. RECONSTRUCT FINAL PATH
        // =====================================================

        List<String> path =
                reconstructPath(
                        sourceNode,
                        destinationNode,
                        previousNodes
                );


        // =====================================================
        // 19. CALCULATE FINAL TRAFFIC-ADJUSTED TRAVEL TIME
        // =====================================================

        double estimatedTravelTimeMinutes =
                calculateTotalTravelTime(
                        path
                );


        // =====================================================
        // 20. CALCULATE OVERALL TRAFFIC LEVEL
        // =====================================================

        String overallTrafficLevel =
                calculateOverallTrafficLevel(
                        path
                );


        // =====================================================
        // 21. RETURN RESULT
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
    // A* HEURISTIC
    // =========================================================
    //
    // We calculate:
    //
    //      straight-line distance
    //                    ----------------
    //                    maximum graph speed
    //
    // and convert the result into minutes.
    //
    // The straight-line distance is calculated using Haversine.
    //
    // The maximum speed comes from the actual road graph,
    // rather than using an arbitrary hard-coded value.
    //
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


        // Safety check.

        if (
                current == null
                        || destination == null
        ) {

            return 0.0;
        }


        // Calculate straight-line distance.

        double straightLineDistance =
                haversineDistance(
                        current.latitude(),
                        current.longitude(),
                        destination.latitude(),
                        destination.longitude()
                );


        // Get maximum speed from the graph.

        double maximumGraphSpeed =
                getMaximumGraphSpeedKmh();


        // Convert distance at maximum speed into minutes.

        return (
                straightLineDistance
                        / maximumGraphSpeed
        ) * 60.0;
    }


    // =========================================================
    // GET MAXIMUM SPEED FROM GRAPH
    // =========================================================
    //
    // We calculate the speed of every edge:
    //
    //      speed = distance / time
    //
    // Then take the maximum.
    //
    // The result is cached so this expensive operation only
    // happens once during the application's lifetime.
    //
    // =========================================================

    private double getMaximumGraphSpeedKmh() {

        // -----------------------------------------------------
        // Return cached value if already calculated.
        // -----------------------------------------------------

        if (maximumGraphSpeedKmh != null) {

            return maximumGraphSpeedKmh;
        }


        double maximumSpeed =
                0.0;


        // -----------------------------------------------------
        // Scan every node.
        // -----------------------------------------------------

        for (GraphNode node :
                roadGraph.getNodes().values()) {

            List<GraphEdge> neighbors =
                    roadGraph.getNeighbors(
                            node.id()
                    );


            // -------------------------------------------------
            // Scan every outgoing edge.
            // -------------------------------------------------

            for (GraphEdge edge :
                    neighbors) {

                // Avoid division by zero.

                if (edge.travelTimeMinutes() <= 0) {

                    continue;
                }


                // Calculate speed represented by this edge.

                double speedKmh =
                        edge.distanceKm()
                                /
                                (
                                        edge.travelTimeMinutes()
                                                / 60.0
                                );


                // Keep the maximum speed.

                maximumSpeed =
                        Math.max(
                                maximumSpeed,
                                speedKmh
                        );
            }
        }


        // -----------------------------------------------------
        // Make sure a valid speed was found.
        // -----------------------------------------------------

        if (maximumSpeed <= 0.0) {

            throw new RuntimeException(
                    "Unable to determine maximum graph speed"
            );
        }


        // -----------------------------------------------------
        // Cache the value.
        // -----------------------------------------------------

        maximumGraphSpeedKmh =
                maximumSpeed;


        // -----------------------------------------------------
        // Print it once so we can verify it.
        // -----------------------------------------------------

        System.out.println(
                "A* maximum graph speed: "
                        + maximumGraphSpeedKmh
                        + " km/h"
        );


        return maximumGraphSpeedKmh;
    }


    // =========================================================
    // HAVERSINE DISTANCE
    // =========================================================
    //
    // Calculates straight-line distance between two
    // geographical coordinates.
    //
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


        // Walk backwards from destination to source.

        while (current != null) {

            path.add(
                    current
            );


            // We reached the source.

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


        // Reverse the path so it becomes:
        //
        // source -> ... -> destination

        Collections.reverse(
                path
        );


        // Validate reconstructed path.

        if (
                path.isEmpty()
                        || !path.get(0).equals(
                        sourceNode
                )
        ) {

            throw new RuntimeException(
                    "Unable to reconstruct A* route"
            );
        }


        return path;
    }


    // =========================================================
    // CALCULATE TOTAL TRAVEL TIME
    // =========================================================

    private double calculateTotalTravelTime(
            List<String> path
    ) {

        double totalTimeMinutes =
                0.0;


        // Every pair of consecutive nodes represents
        // one road edge.

        for (
                int i = 0;
                i < path.size() - 1;
                i++
        ) {

            String source =
                    path.get(i);


            String destination =
                    path.get(i + 1);


            // Find the actual graph edge.

            GraphEdge edge =
                    findEdge(
                            source,
                            destination
                    );


            // Get traffic multiplier for this edge.

            double trafficMultiplier =
                    trafficService.getTrafficMultiplier(
                            source,
                            destination
                    );


            // Add traffic-adjusted travel time.

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
    // CALCULATE OVERALL TRAFFIC LEVEL
    // =========================================================

    private String calculateOverallTrafficLevel(
            List<String> path
    ) {

        // No roads means LOW traffic.

        if (path.size() < 2) {

            return "LOW";
        }


        double totalMultiplier =
                0.0;


        int roadCount =
                path.size() - 1;


        // Add traffic multiplier for every road.

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


        // Calculate average traffic multiplier.

        double averageMultiplier =
                totalMultiplier
                        / roadCount;


        // Convert multiplier into traffic label.

        return trafficService.getTrafficLevel(
                averageMultiplier
        );
    }


    // =========================================================
    // ROUND TO TWO DECIMAL PLACES
    // =========================================================

    private double roundToTwoDecimals(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    // =========================================================
    // PRIORITY QUEUE NODE
    // =========================================================

    private record NodeScore(

            String nodeId,

            double gScore,

            double fScore

    ) {
    }
}