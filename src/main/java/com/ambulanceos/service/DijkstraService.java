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
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class DijkstraService {

    private final GurgaonRoadGraph roadGraph;


    // =========================================================
    // DIJKSTRA SHORTEST PATH
    // =========================================================
    //
    // Finds the shortest path between two OSM road nodes.
    //
    // Weight:
    //
    //     edge.distanceKm()
    //
    // Data structure:
    //
    //     PriorityQueue
    //
    // Graph:
    //
    //     Real Gurgaon OSM road graph
    //
    // The graph can contain hundreds of thousands of nodes,
    // therefore we only store nodes discovered by Dijkstra
    // inside the HashMaps.
    // =========================================================

    public DijkstraResponse findShortestPath(

            String sourceNode,

            String destinationNode

    ) {

        // =====================================================
        // 1. VALIDATE SOURCE NODE
        // =====================================================

        if (
                roadGraph.getNode(sourceNode) == null
        ) {

            throw new RuntimeException(
                    "Source node not found: "
                            + sourceNode
            );
        }


        // =====================================================
        // 2. VALIDATE DESTINATION NODE
        // =====================================================

        if (
                roadGraph.getNode(destinationNode) == null
        ) {

            throw new RuntimeException(
                    "Destination node not found: "
                            + destinationNode
            );
        }


        // =====================================================
        // 3. DISTANCE MAP
        // =====================================================
        //
        // Stores the shortest known distance from the source
        // to every discovered node.
        //
        // Example:
        //
        // source -> A = 1.2 km
        // source -> B = 2.7 km
        // source -> C = 4.1 km
        //
        // =====================================================

        Map<String, Double> distances =
                new HashMap<>();


        // =====================================================
        // 4. PREVIOUS NODE MAP
        // =====================================================
        //
        // Used to reconstruct the final shortest path.
        //
        // Example:
        //
        // C <- B <- A <- SOURCE
        //
        // previousNodes contains:
        //
        // C -> B
        // B -> A
        // A -> SOURCE
        //
        // =====================================================

        Map<String, String> previousNodes =
                new HashMap<>();


        // =====================================================
        // 5. PRIORITY QUEUE
        // =====================================================
        //
        // Always processes the node with the smallest
        // currently known distance.
        //
        // This is the main data structure that makes
        // Dijkstra efficient.
        //
        // =====================================================

        PriorityQueue<NodeDistance> priorityQueue =
                new PriorityQueue<>(
                        Comparator.comparingDouble(
                                NodeDistance::distance
                        )
                );


        // =====================================================
        // 6. INITIALIZE SOURCE
        // =====================================================

        distances.put(
                sourceNode,
                0.0
        );


        priorityQueue.offer(
                new NodeDistance(
                        sourceNode,
                        0.0
                )
        );


        // =====================================================
        // 7. MAIN DIJKSTRA LOOP
        // =====================================================

        while (
                !priorityQueue.isEmpty()
        ) {

            // -------------------------------------------------
            // Get node with smallest distance.
            // -------------------------------------------------

            NodeDistance current =
                    priorityQueue.poll();


            String currentNode =
                    current.nodeId();


            double currentDistance =
                    current.distance();


            // -------------------------------------------------
            // STALE ENTRY CHECK
            // -------------------------------------------------
            //
            // A node may appear multiple times in the
            // PriorityQueue.
            //
            // Example:
            //
            // B = 10 km
            //
            // Later we discover:
            //
            // B = 6 km
            //
            // Both entries remain in the queue.
            //
            // When the old 10 km entry comes out, we ignore it.
            //
            // -------------------------------------------------

            double bestKnownDistance =
                    distances.getOrDefault(
                            currentNode,
                            Double.POSITIVE_INFINITY
                    );


            if (
                    currentDistance
                            > bestKnownDistance
            ) {

                continue;
            }


            // -------------------------------------------------
            // DESTINATION REACHED
            // -------------------------------------------------
            //
            // Because this is a min PriorityQueue, when the
            // destination is removed from the queue, its
            // shortest distance is finalized.
            //
            // -------------------------------------------------

            if (
                    currentNode.equals(
                            destinationNode
                    )
            ) {

                break;
            }


            // =================================================
            // 8. GET NEIGHBORING ROAD SEGMENTS
            // =================================================

            List<GraphEdge> neighbors =
                    roadGraph.getNeighbors(
                            currentNode
                    );


            // =================================================
            // 9. RELAX EVERY EDGE
            // =================================================

            for (
                    GraphEdge edge :
                    neighbors
            ) {

                String neighbor =
                        edge.destinationNodeId();


                // -------------------------------------------------
                // Calculate distance through current node.
                // -------------------------------------------------

                double newDistance =
                        currentDistance
                                + edge.distanceKm();


                // -------------------------------------------------
                // Current best known distance to neighbor.
                // -------------------------------------------------

                double knownDistance =
                        distances.getOrDefault(
                                neighbor,
                                Double.POSITIVE_INFINITY
                        );


                // -------------------------------------------------
                // RELAXATION
                // -------------------------------------------------
                //
                // If this route is shorter:
                //
                // 1. Update distance
                // 2. Store previous node
                // 3. Add new value to PriorityQueue
                //
                // -------------------------------------------------

                if (
                        newDistance
                                < knownDistance
                ) {

                    distances.put(
                            neighbor,
                            newDistance
                    );


                    previousNodes.put(
                            neighbor,
                            currentNode
                    );


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
        // 10. GET FINAL DISTANCE
        // =====================================================

        Double shortestDistance =
                distances.get(
                        destinationNode
                );


        // -----------------------------------------------------
        // No route found
        // -----------------------------------------------------

        if (
                shortestDistance == null
                        || Double.isInfinite(
                        shortestDistance
                )
        ) {

            throw new RuntimeException(
                    "No route exists between "
                            + sourceNode
                            + " and "
                            + destinationNode
            );
        }


        // =====================================================
        // 11. RECONSTRUCT PATH
        // =====================================================
        //
        // IMPORTANT:
        //
        // We return the actual OSM node IDs directly.
        //
        // There is NO:
        //
        // coordinates -> node ID
        //
        // conversion anymore.
        //
        // Therefore there is no UNKNOWN problem.
        //
        // =====================================================

        List<String> path =
                reconstructPath(

                        sourceNode,

                        destinationNode,

                        previousNodes

                );


        // =====================================================
        // 12. RETURN DIJKSTRA RESULT
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
    // RECONSTRUCT SHORTEST PATH
    // =========================================================
    //
    // We start from the destination and move backwards using
    // previousNodes.
    //
    // Example:
    //
    // destination
    //      ↑
    //      |
    //     C
    //      ↑
    //      |
    //     B
    //      ↑
    //      |
    //     A
    //      ↑
    //      |
    //    SOURCE
    //
    // Backwards:
    //
    // destination -> C -> B -> A -> source
    //
    // Then reverse it:
    //
    // source -> A -> B -> C -> destination
    //
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
        // Walk backwards.
        // -----------------------------------------------------

        while (
                current != null
        ) {

            path.add(
                    current
            );


            // -------------------------------------------------
            // We reached the source.
            // -------------------------------------------------

            if (
                    current.equals(
                            sourceNode
                    )
            ) {

                break;
            }


            current =
                    previousNodes.get(
                            current
                    );
        }


        // -----------------------------------------------------
        // Reverse path.
        // -----------------------------------------------------

        Collections.reverse(
                path
        );


        // -----------------------------------------------------
        // Safety check.
        // -----------------------------------------------------

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
    // PRIORITY QUEUE NODE
    // =========================================================

    private record NodeDistance(

            String nodeId,

            double distance

    ) {
    }
}