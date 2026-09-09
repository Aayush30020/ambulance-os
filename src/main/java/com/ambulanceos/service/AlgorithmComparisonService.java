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

    /*
     * ---------------------------------------------------------
     * BENCHMARK CONFIGURATION
     * ---------------------------------------------------------
     */

    private static final int WARMUP_RUNS = 2;

    private static final int MEASURED_RUNS = 10;


    private final GurgaonRoadGraph roadGraph;

    private final TrafficService trafficService;


    /*
     * Cached maximum graph speed used by the A* heuristic.
     *
     * We calculate this only once because the graph does not
     * change during application execution.
     */
    private Double maximumGraphSpeedKmh;


    // =========================================================
    // MAIN COMPARISON
    // =========================================================

    public AlgorithmComparisonResponse compare(
            String sourceNode,
            String destinationNode
    ) {

        // -----------------------------------------------------
        // Validate source
        // -----------------------------------------------------

        validateNode(
                sourceNode,
                "Source"
        );


        // -----------------------------------------------------
        // Validate destination
        // -----------------------------------------------------

        validateNode(
                destinationNode,
                "Destination"
        );


        /*
         * =====================================================
         * WARM-UP
         * =====================================================
         *
         * JVM/JIT compilation and CPU/cache effects can make
         * the first few executions slower.
         *
         * Therefore we execute both algorithms first without
         * including these runs in the benchmark statistics.
         */

        for (int i = 0; i < WARMUP_RUNS; i++) {

            runDijkstra(
                    sourceNode,
                    destinationNode
            );

            runAStar(
                    sourceNode,
                    destinationNode
            );
        }


        /*
         * =====================================================
         * MEASURED RUNS
         * =====================================================
         */

        List<SearchResult> dijkstraResults =
                new ArrayList<>();


        List<SearchResult> aStarResults =
                new ArrayList<>();


        /*
         * Alternate algorithm order on every iteration.
         *
         * This reduces systematic ordering bias where the
         * algorithm executed first might receive slightly
         * different CPU/cache conditions.
         */

        for (int i = 0; i < MEASURED_RUNS; i++) {

            if (i % 2 == 0) {

                dijkstraResults.add(
                        runDijkstra(
                                sourceNode,
                                destinationNode
                        )
                );

                aStarResults.add(
                        runAStar(
                                sourceNode,
                                destinationNode
                        )
                );

            } else {

                aStarResults.add(
                        runAStar(
                                sourceNode,
                                destinationNode
                        )
                );

                dijkstraResults.add(
                        runDijkstra(
                                sourceNode,
                                destinationNode
                        )
                );
            }
        }


        /*
         * =====================================================
         * REPRESENTATIVE RESULTS
         * =====================================================
         *
         * The route itself should be deterministic because the
         * graph and traffic model are deterministic.
         *
         * We use the first measured run as the representative
         * route/result shown in the existing UI.
         */

        SearchResult dijkstraRepresentative =
                dijkstraResults.get(0);


        SearchResult aStarRepresentative =
                aStarResults.get(0);


        /*
         * =====================================================
         * VERIFY OPTIMALITY
         * =====================================================
         */

        boolean sameDistance =
                Math.abs(
                        dijkstraRepresentative.distanceKm()
                                - aStarRepresentative.distanceKm()
                ) < 0.0001;


        boolean sameTravelTime =
                Math.abs(
                        dijkstraRepresentative.travelTimeMinutes()
                                - aStarRepresentative.travelTimeMinutes()
                ) < 0.0001;


        boolean samePath =
                dijkstraRepresentative.path()
                        .equals(
                                aStarRepresentative.path()
                        );


        /*
         * =====================================================
         * CALCULATE BENCHMARK STATISTICS
         * =====================================================
         */

        BenchmarkStatistics dijkstraStats =
                calculateStatistics(
                        dijkstraResults
                );


        BenchmarkStatistics aStarStats =
                calculateStatistics(
                        aStarResults
                );


        /*
         * =====================================================
         * RETURN RESPONSE
         * =====================================================
         */

        return new AlgorithmComparisonResponse(

                sourceNode,

                destinationNode,


                new AlgorithmComparisonResponse.AlgorithmResult(

                        "TRAFFIC_DIJKSTRA",

                        roundToTwoDecimals(
                                dijkstraRepresentative.distanceKm()
                        ),

                        roundToTwoDecimals(
                                dijkstraRepresentative.travelTimeMinutes()
                        ),

                        dijkstraRepresentative.trafficLevel(),

                        (int) Math.round(
                                dijkstraStats.averageNodesExplored()
                        ),

                        Math.round(
                                dijkstraStats.averageExecutionTimeMillis()
                        ),

                        dijkstraRepresentative.path(),

                        roundToTwoDecimals(
                                dijkstraStats.averageExecutionTimeMillis()
                        ),

                        roundToTwoDecimals(
                                dijkstraStats.medianExecutionTimeMillis()
                        ),

                        dijkstraStats.minimumExecutionTimeMillis(),

                        dijkstraStats.maximumExecutionTimeMillis(),

                        roundToTwoDecimals(
                                dijkstraStats.averageNodesExplored()
                        )
                ),


                new AlgorithmComparisonResponse.AlgorithmResult(

                        "A_STAR",

                        roundToTwoDecimals(
                                aStarRepresentative.distanceKm()
                        ),

                        roundToTwoDecimals(
                                aStarRepresentative.travelTimeMinutes()
                        ),

                        aStarRepresentative.trafficLevel(),

                        (int) Math.round(
                                aStarStats.averageNodesExplored()
                        ),

                        Math.round(
                                aStarStats.averageExecutionTimeMillis()
                        ),

                        aStarRepresentative.path(),

                        roundToTwoDecimals(
                                aStarStats.averageExecutionTimeMillis()
                        ),

                        roundToTwoDecimals(
                                aStarStats.medianExecutionTimeMillis()
                        ),

                        aStarStats.minimumExecutionTimeMillis(),

                        aStarStats.maximumExecutionTimeMillis(),

                        roundToTwoDecimals(
                                aStarStats.averageNodesExplored()
                        )
                ),


                sameDistance,

                sameTravelTime,

                samePath,

                WARMUP_RUNS,

                MEASURED_RUNS
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


            /*
             * Ignore stale PriorityQueue entries.
             */
            if (currentCost > bestKnownCost) {
                continue;
            }


            /*
             * Ignore already processed nodes.
             */
            if (processedNodes.contains(
                    currentNode
            )) {
                continue;
            }


            processedNodes.add(
                    currentNode
            );


            /*
             * Once the destination is removed from the
             * PriorityQueue, its shortest cost is final.
             */
            if (currentNode.equals(
                    destinationNode
            )) {
                break;
            }


            /*
             * Explore every outgoing road.
             */
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


                /*
                 * Get deterministic traffic multiplier.
                 */
                double trafficMultiplier =
                        trafficService.getTrafficMultiplier(
                                currentNode,
                                neighbor
                        );


                /*
                 * Dijkstra optimizes traffic-adjusted
                 * travel time.
                 */
                double trafficAdjustedTime =
                        baseTravelTime
                                * trafficMultiplier;


                double newCost =
                        currentCost
                                + trafficAdjustedTime;


                /*
                 * Physical road distance is tracked
                 * separately from the optimization cost.
                 */
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


                /*
                 * Relaxation step.
                 */
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


        /*
         * Destination was not reached.
         */
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


        /*
         * Reconstruct shortest path.
         */
        List<String> path =
                reconstructPath(
                        sourceNode,
                        destinationNode,
                        previousNodes
                );


        /*
         * Calculate final traffic-adjusted travel time.
         */
        double travelTime =
                calculateTravelTime(
                        path
                );


        /*
         * Calculate overall traffic level.
         */
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


        /*
         * gScore = actual traffic-adjusted travel time
         * from source to current node.
         */
        gCosts.put(
                sourceNode,
                0.0
        );


        distances.put(
                sourceNode,
                0.0
        );


        /*
         * Calculate initial heuristic.
         */
        double sourceHeuristic =
                heuristicMinutes(
                        sourceNode,
                        destinationNode
                );


        /*
         * fScore = gScore + heuristic.
         */
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


            /*
             * Ignore already processed nodes.
             */
            if (processedNodes.contains(
                    currentNode
            )) {
                continue;
            }


            processedNodes.add(
                    currentNode
            );


            /*
             * Destination reached.
             */
            if (currentNode.equals(
                    destinationNode
            )) {
                break;
            }


            /*
             * Explore neighboring roads.
             */
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


                /*
                 * Same traffic model used by Dijkstra.
                 */
                double trafficMultiplier =
                        trafficService.getTrafficMultiplier(
                                currentNode,
                                neighbor
                        );


                /*
                 * Same optimization cost used by
                 * Traffic-Aware Dijkstra.
                 */
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


                /*
                 * Track physical distance separately.
                 */
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


                /*
                 * Relaxation.
                 */
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


                    /*
                     * Estimate remaining travel time
                     * from neighbor to destination.
                     */
                    double heuristic =
                            heuristicMinutes(
                                    neighbor,
                                    destinationNode
                            );


                    /*
                     * A* priority:
                     *
                     * f(n) = g(n) + h(n)
                     */
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


        /*
         * Destination was not reached.
         */
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


        /*
         * Reconstruct route.
         */
        List<String> path =
                reconstructPath(
                        sourceNode,
                        destinationNode,
                        previousNodes
                );


        /*
         * Calculate final travel time.
         */
        double travelTime =
                calculateTravelTime(
                        path
                );


        /*
         * Calculate traffic level.
         */
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


        /*
         * Straight-line geographic distance.
         */
        double straightLineDistance =
                haversineDistance(
                        current.latitude(),
                        current.longitude(),
                        destination.latitude(),
                        destination.longitude()
                );


        /*
         * Use maximum observed graph speed as the
         * lower-bound travel speed.
         *
         * This makes the heuristic admissible because
         * actual road travel cannot be faster than the
         * fastest base road speed represented in the graph.
         */
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

        /*
         * Calculate only once.
         */
        if (maximumGraphSpeedKmh != null) {

            return maximumGraphSpeedKmh;
        }


        double maximumSpeed =
                0.0;


        /*
         * Inspect every graph edge and determine its
         * implied base speed.
         */
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
    // BENCHMARK STATISTICS
    // =========================================================

    private BenchmarkStatistics calculateStatistics(
            List<SearchResult> results
    ) {

        /*
         * Extract execution times.
         */
        List<Long> executionTimes =
                results.stream()
                        .map(
                                SearchResult::executionTimeMillis
                        )
                        .sorted()
                        .toList();


        /*
         * Extract nodes explored.
         */
        double averageNodes =
                results.stream()
                        .mapToInt(
                                SearchResult::nodesExplored
                        )
                        .average()
                        .orElse(0.0);


        /*
         * Average execution time.
         */
        double averageExecutionTime =
                executionTimes.stream()
                        .mapToLong(
                                Long::longValue
                        )
                        .average()
                        .orElse(0.0);


        /*
         * Median execution time.
         */
        double medianExecutionTime;


        int size =
                executionTimes.size();


        if (size % 2 == 0) {

            long first =
                    executionTimes.get(
                            size / 2 - 1
                    );


            long second =
                    executionTimes.get(
                            size / 2
                    );


            medianExecutionTime =
                    (first + second) / 2.0;

        } else {

            medianExecutionTime =
                    executionTimes.get(
                            size / 2
                    );
        }


        long minimumExecutionTime =
                executionTimes.get(0);


        long maximumExecutionTime =
                executionTimes.get(
                        executionTimes.size() - 1
                );


        return new BenchmarkStatistics(

                averageExecutionTime,

                medianExecutionTime,

                minimumExecutionTime,

                maximumExecutionTime,

                averageNodes
        );
    }


    // =========================================================
    // VALIDATE NODE
    // =========================================================

    private void validateNode(
            String nodeId,
            String label
    ) {

        if (roadGraph.getNode(
                nodeId
        ) == null) {

            throw new RuntimeException(
                    label
                            + " node not found: "
                            + nodeId
            );
        }
    }


    // =========================================================
    // HAVERSINE DISTANCE
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
    // SEARCH RESULT
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
    // BENCHMARK STATISTICS
    // =========================================================

    private record BenchmarkStatistics(

            double averageExecutionTimeMillis,

            double medianExecutionTimeMillis,

            long minimumExecutionTimeMillis,

            long maximumExecutionTimeMillis,

            double averageNodesExplored

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