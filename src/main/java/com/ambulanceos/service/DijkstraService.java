package com.ambulanceos.service;

import com.ambulanceos.dto.DijkstraResponse;
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
public class DijkstraService {

    private final GurgaonRoadGraph roadGraph;


    // =========================================================
    // DIJKSTRA SHORTEST PATH
    // =========================================================

    public DijkstraResponse findShortestPath(

            String sourceNode,

            String destinationNode

    ) {

        // -----------------------------------------------------
        // 1. Validate source node
        // -----------------------------------------------------

        if (roadGraph.getNode(sourceNode) == null) {

            throw new RuntimeException(
                    "Source node not found: " + sourceNode
            );
        }


        // -----------------------------------------------------
        // 2. Validate destination node
        // -----------------------------------------------------

        if (roadGraph.getNode(destinationNode) == null) {

            throw new RuntimeException(
                    "Destination node not found: "
                            + destinationNode
            );
        }


        // -----------------------------------------------------
        // 3. Distance map
        //
        // Stores the shortest known distance from source
        // to every node.
        // -----------------------------------------------------

        Map<String, Double> distances =
                new HashMap<>();


        // -----------------------------------------------------
        // 4. Previous node map
        //
        // Used later to reconstruct the shortest path.
        // -----------------------------------------------------

        Map<String, String> previousNodes =
                new HashMap<>();


        // -----------------------------------------------------
        // 5. Initialize distances
        //
        // Initially every node is considered infinitely far
        // away from the source.
        // -----------------------------------------------------

        for (String nodeId :
                roadGraph.getNodes().keySet()) {

            distances.put(
                    nodeId,
                    Double.POSITIVE_INFINITY
            );
        }


        // -----------------------------------------------------
        // 6. Source distance = 0
        // -----------------------------------------------------

        distances.put(
                sourceNode,
                0.0
        );


        // -----------------------------------------------------
        // 7. PriorityQueue
        //
        // The node with the smallest current distance
        // gets processed first.
        // -----------------------------------------------------

        PriorityQueue<NodeDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                NodeDistance::distance
                        )
                );


        // -----------------------------------------------------
        // 8. Add source node
        // -----------------------------------------------------

        priorityQueue.offer(
                new NodeDistance(
                        sourceNode,
                        0.0
                )
        );


        // -----------------------------------------------------
        // 9. Track processed nodes
        // -----------------------------------------------------

        Set<String> processedNodes =
                new HashSet<>();


        // =====================================================
        // 10. MAIN DIJKSTRA LOOP
        // =====================================================

        while (!priorityQueue.isEmpty()) {

            // -------------------------------------------------
            // Get node with smallest known distance.
            // -------------------------------------------------

            NodeDistance current =
                    priorityQueue.poll();


            String currentNode =
                    current.nodeId();


            double currentDistance =
                    current.distance();


            // -------------------------------------------------
            // Skip if this node was already processed.
            // -------------------------------------------------

            if (processedNodes.contains(
                    currentNode
            )) {

                continue;
            }


            // -------------------------------------------------
            // Mark node as processed.
            // -------------------------------------------------

            processedNodes.add(
                    currentNode
            );


            // -------------------------------------------------
            // If destination is reached, we can stop.
            // -------------------------------------------------

            if (currentNode.equals(
                    destinationNode
            )) {

                break;
            }


            // -------------------------------------------------
            // Get all neighboring roads.
            // -------------------------------------------------

            List<GraphEdge> neighbors =
                    roadGraph.getNeighbors(
                            currentNode
                    );


            // -------------------------------------------------
            // Relax every neighboring edge.
            // -------------------------------------------------

            for (GraphEdge edge :
                    neighbors) {

                String neighbor =
                        edge.destinationNodeId();


                double newDistance =
                        currentDistance
                                + edge.distanceKm();


                // -------------------------------------------------
                // RELAXATION
                //
                // If going through currentNode gives us a shorter
                // path to neighbor, update it.
                // -------------------------------------------------

                if (newDistance <
                        distances.get(neighbor)) {

                    distances.put(
                            neighbor,
                            newDistance
                    );


                    previousNodes.put(
                            neighbor,
                            currentNode
                    );


                    // Add updated distance to PriorityQueue.

                    priorityQueue.offer(
                            new NodeDistance(
                                    neighbor,
                                    newDistance
                            )
                    );
                }
            }
        }


        // =====================================================
        // 11. CHECK WHETHER DESTINATION WAS REACHED
        // =====================================================

        double shortestDistance =
                distances.get(destinationNode);


        if (Double.isInfinite(
                shortestDistance
        )) {

            throw new RuntimeException(
                    "No route exists between "
                            + sourceNode
                            + " and "
                            + destinationNode
            );
        }


        // =====================================================
        // 12. RECONSTRUCT SHORTEST PATH
        // =====================================================

        List<String> path =
                reconstructPath(

                        sourceNode,

                        destinationNode,

                        previousNodes

                );


        // =====================================================
        // 13. RETURN RESULT
        // =====================================================

        return new DijkstraResponse(

                sourceNode,

                destinationNode,

                roundToTwoDecimals(
                        shortestDistance
                ),

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


        // -----------------------------------------------------
        // Walk backwards from destination to source.
        // -----------------------------------------------------

        while (current != null) {

            path.add(current);


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


        // -----------------------------------------------------
        // Reverse because we constructed the path backwards.
        // -----------------------------------------------------

        Collections.reverse(
                path
        );


        // -----------------------------------------------------
        // Safety check.
        // -----------------------------------------------------

        if (path.isEmpty()
                || !path.get(0).equals(
                sourceNode
        )) {

            throw new RuntimeException(
                    "Unable to reconstruct route"
            );
        }


        return path;
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

    private record NodeDistance(

            String nodeId,

            double distance

    ) {
    }
}