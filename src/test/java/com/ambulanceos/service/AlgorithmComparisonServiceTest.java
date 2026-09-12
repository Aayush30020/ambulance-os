package com.ambulanceos.service;

import com.ambulanceos.dto.AlgorithmComparisonResponse;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AlgorithmComparisonServiceTest {

    @Autowired
    private AlgorithmComparisonService algorithmComparisonService;

    @Autowired
    private GurgaonRoadGraph roadGraph;


    private String sourceNode;
    private String destinationNode;


    @BeforeEach
    void setUp() {

        Map<String, GraphNode> nodes =
                roadGraph.getNodes();

        assertNotNull(nodes);
        assertFalse(
                nodes.isEmpty(),
                "Gurgaon road graph should not be empty"
        );

        /*
         * Find two connected nodes dynamically.
         *
         * We intentionally do not hard-code OSM node IDs because
         * those IDs belong to the imported Gurgaon graph and may
         * change if the graph is regenerated.
         */
        sourceNode =
                nodes.keySet()
                        .stream()
                        .findFirst()
                        .orElseThrow();

        destinationNode =
                findReachableDestination(
                        sourceNode
                );

        assertNotNull(destinationNode);

        assertNotEquals(
                sourceNode,
                destinationNode
        );
    }


    // =========================================================
    // GRAPH SHOULD CONTAIN NODES
    // =========================================================

    @Test
    void shouldLoadGurgaonRoadGraph() {

        assertNotNull(
                roadGraph.getNodes()
        );

        assertFalse(
                roadGraph.getNodes().isEmpty()
        );
    }


    // =========================================================
    // DIJKSTRA VS A*
    // =========================================================

    @Test
    void shouldProduceSameOptimalResultForDijkstraAndAStar() {

        AlgorithmComparisonResponse result =
                algorithmComparisonService.compare(
                        sourceNode,
                        destinationNode
                );

        assertNotNull(result);

        // -----------------------------------------------------
        // Source / destination
        // -----------------------------------------------------

        assertEquals(
                sourceNode,
                result.sourceNode()
        );

        assertEquals(
                destinationNode,
                result.destinationNode()
        );

        // -----------------------------------------------------
        // Algorithm results
        // -----------------------------------------------------

        assertNotNull(
                result.dijkstra()
        );

        assertNotNull(
                result.aStar()
        );

        assertEquals(
                "TRAFFIC_DIJKSTRA",
                result.dijkstra().algorithm()
        );

        assertEquals(
                "A_STAR",
                result.aStar().algorithm()
        );

        // -----------------------------------------------------
        // Optimality
        // -----------------------------------------------------

        assertTrue(
                result.sameOptimalDistance(),
                "Dijkstra and A* should return the same optimal distance"
        );

        assertTrue(
                result.sameOptimalTravelTime(),
                "Dijkstra and A* should return the same optimal travel time"
        );

        assertTrue(
                result.samePath(),
                "Dijkstra and A* should return the same optimal path"
        );
    }


    // =========================================================
    // DIJKSTRA RESULT
    // =========================================================

    @Test
    void shouldReturnValidDijkstraBenchmarkResult() {

        AlgorithmComparisonResponse result =
                algorithmComparisonService.compare(
                        sourceNode,
                        destinationNode
                );

        AlgorithmComparisonResponse.AlgorithmResult dijkstra =
                result.dijkstra();

        assertNotNull(dijkstra);

        assertEquals(
                "TRAFFIC_DIJKSTRA",
                dijkstra.algorithm()
        );

        assertTrue(
                dijkstra.distanceKm() >= 0
        );

        assertTrue(
                dijkstra.estimatedTravelTimeMinutes() >= 0
        );

        assertNotNull(
                dijkstra.trafficLevel()
        );

        assertTrue(
                dijkstra.nodesExplored() > 0
        );

        assertTrue(
                dijkstra.executionTimeMillis() >= 0
        );

        assertNotNull(
                dijkstra.path()
        );

        assertFalse(
                dijkstra.path().isEmpty()
        );

        assertTrue(
                dijkstra.averageExecutionTimeMillis() >= 0
        );

        assertTrue(
                dijkstra.medianExecutionTimeMillis() >= 0
        );

        assertTrue(
                dijkstra.minimumExecutionTimeMillis() >= 0
        );

        assertTrue(
                dijkstra.maximumExecutionTimeMillis() >= 0
        );

        assertTrue(
                dijkstra.averageNodesExplored() > 0
        );
    }


    // =========================================================
    // A* RESULT
    // =========================================================

    @Test
    void shouldReturnValidAStarBenchmarkResult() {

        AlgorithmComparisonResponse result =
                algorithmComparisonService.compare(
                        sourceNode,
                        destinationNode
                );

        AlgorithmComparisonResponse.AlgorithmResult aStar =
                result.aStar();

        assertNotNull(aStar);

        assertEquals(
                "A_STAR",
                aStar.algorithm()
        );

        assertTrue(
                aStar.distanceKm() >= 0
        );

        assertTrue(
                aStar.estimatedTravelTimeMinutes() >= 0
        );

        assertNotNull(
                aStar.trafficLevel()
        );

        assertTrue(
                aStar.nodesExplored() > 0
        );

        assertTrue(
                aStar.executionTimeMillis() >= 0
        );

        assertNotNull(
                aStar.path()
        );

        assertFalse(
                aStar.path().isEmpty()
        );

        assertTrue(
                aStar.averageExecutionTimeMillis() >= 0
        );

        assertTrue(
                aStar.medianExecutionTimeMillis() >= 0
        );

        assertTrue(
                aStar.minimumExecutionTimeMillis() >= 0
        );

        assertTrue(
                aStar.maximumExecutionTimeMillis() >= 0
        );

        assertTrue(
                aStar.averageNodesExplored() > 0
        );
    }


    // =========================================================
    // BENCHMARK CONFIGURATION
    // =========================================================

    @Test
    void shouldUseExpectedBenchmarkConfiguration() {

        AlgorithmComparisonResponse result =
                algorithmComparisonService.compare(
                        sourceNode,
                        destinationNode
                );

        assertEquals(
                2,
                result.warmupRuns()
        );

        assertEquals(
                10,
                result.measuredRuns()
        );
    }


    // =========================================================
    // RESULT PATHS SHOULD START AND END CORRECTLY
    // =========================================================

    @Test
    void shouldReturnPathsFromSourceToDestination() {

        AlgorithmComparisonResponse result =
                algorithmComparisonService.compare(
                        sourceNode,
                        destinationNode
                );

        assertFalse(
                result.dijkstra()
                        .path()
                        .isEmpty()
        );

        assertFalse(
                result.aStar()
                        .path()
                        .isEmpty()
        );

        assertEquals(
                sourceNode,
                result.dijkstra()
                        .path()
                        .get(0)
        );

        assertEquals(
                destinationNode,
                result.dijkstra()
                        .path()
                        .get(
                                result.dijkstra()
                                        .path()
                                        .size() - 1
                        )
        );

        assertEquals(
                sourceNode,
                result.aStar()
                        .path()
                        .get(0)
        );

        assertEquals(
                destinationNode,
                result.aStar()
                        .path()
                        .get(
                                result.aStar()
                                        .path()
                                        .size() - 1
                        )
        );
    }


    // =========================================================
    // INVALID SOURCE
    // =========================================================

    @Test
    void shouldRejectInvalidSourceNode() {

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> algorithmComparisonService.compare(
                                "INVALID_SOURCE_NODE",
                                destinationNode
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Source node not found"
                        )
        );
    }


    // =========================================================
    // INVALID DESTINATION
    // =========================================================

    @Test
    void shouldRejectInvalidDestinationNode() {

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> algorithmComparisonService.compare(
                                sourceNode,
                                "INVALID_DESTINATION_NODE"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Destination node not found"
                        )
        );
    }


    // =========================================================
    // FIND REACHABLE DESTINATION
    // =========================================================

    private String findReachableDestination(
            String source
    ) {

        Queue<String> queue =
                new ArrayDeque<>();

        Set<String> visited =
                new HashSet<>();

        queue.offer(source);
        visited.add(source);

        String lastNode =
                source;

        /*
         * Explore a reasonable section of the graph.
         *
         * This avoids hard-coded OSM IDs while still selecting
         * a destination that is definitely reachable.
         */
        int exploredNodes = 0;

        final int MAX_EXPLORED_NODES = 500;

        while (
                !queue.isEmpty()
                        && exploredNodes < MAX_EXPLORED_NODES
        ) {

            String current =
                    queue.poll();

            lastNode =
                    current;

            exploredNodes++;

            for (
                    var edge :
                    roadGraph.getNeighbors(current)
            ) {

                String neighbor =
                        edge.destinationNodeId();

                if (
                        visited.add(neighbor)
                ) {

                    queue.offer(neighbor);
                }
            }
        }

        if (
                !lastNode.equals(source)
        ) {

            return lastNode;
        }

        /*
         * Fallback: find any node with a direct edge from source.
         */
        for (
                var edge :
                roadGraph.getNeighbors(source)
        ) {

            if (
                    roadGraph.getNode(
                            edge.destinationNodeId()
                    ) != null
            ) {

                return edge.destinationNodeId();
            }
        }

        throw new IllegalStateException(
                "Unable to find a reachable destination node"
        );
    }
}