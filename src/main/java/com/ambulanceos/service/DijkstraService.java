package com.ambulanceos.service;

import com.ambulanceos.dto.DijkstraResponse;
import com.ambulanceos.exception.RouteNotFoundException;
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
    // This endpoint performs basic distance-based Dijkstra.
    // Traffic-aware routing is handled separately by
    // TrafficAwareDijkstraService.
    // =========================================================

    public DijkstraResponse findShortestPath(
            String sourceNode,
            String destinationNode
    ) {

        // =====================================================
        // 1. VALIDATE SOURCE NODE
        // =====================================================

        if (roadGraph.getNode(sourceNode) == null) {

            throw new RouteNotFoundException(
                    "Source node not found: " + sourceNode
            );
        }


        // =====================================================
        // 2. VALIDATE DESTINATION NODE
        // =====================================================

        if (roadGraph.getNode(destinationNode) == null) {

            throw new RouteNotFoundException(
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

        while (!priorityQueue.isEmpty()) {

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
            // A node may appear multiple times in the queue.
            // Ignore entries that are no longer optimal.
            // -------------------------------------------------

            double bestKnownDistance =
                    distances.getOrDefault(
                            currentNode,
                            Double.POSITIVE_INFINITY
                    );

            if (currentDistance > bestKnownDistance) {
                continue;
            }


            // -------------------------------------------------
            // DESTINATION REACHED
            // -------------------------------------------------
            //
            // Since the PriorityQueue is ordered by distance,
            // the first time the destination is removed from
            // the queue, its shortest distance is finalized.
            // -------------------------------------------------

            if (currentNode.equals(destinationNode)) {
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

            for (GraphEdge edge : neighbors) {

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

                if (newDistance < knownDistance) {

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


        // =====================================================
        // 11. NO ROUTE FOUND
        // =====================================================

        if (
                shortestDistance == null
                        || Double.isInfinite(
                        shortestDistance
                )
        ) {

            throw new RouteNotFoundException(
                    "No route exists between "
                            + sourceNode
                            + " and "
                            + destinationNode
            );
        }


        // =====================================================
        // 12. RECONSTRUCT PATH
        // =====================================================

        List<String> path =
                reconstructPath(
                        sourceNode,
                        destinationNode,
                        previousNodes
                );


        // =====================================================
        // 13. RETURN DIJKSTRA RESULT
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


            // -------------------------------------------------
            // Source reached.
            // -------------------------------------------------

            if (current.equals(sourceNode)) {
                break;
            }


            current =
                    previousNodes.get(
                            current
                    );
        }


        // -----------------------------------------------------
        // Reverse path so that it becomes:
        //
        // source -> ... -> destination
        // -----------------------------------------------------

        Collections.reverse(path);


        // -----------------------------------------------------
        // Safety check.
        // -----------------------------------------------------

        if (
                path.isEmpty()
                        || !path.get(0).equals(
                        sourceNode
                )
        ) {

            throw new RouteNotFoundException(
                    "Unable to reconstruct route from "
                            + sourceNode
                            + " to "
                            + destinationNode
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