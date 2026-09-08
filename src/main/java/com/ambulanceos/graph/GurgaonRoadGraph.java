package com.ambulanceos.graph;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GurgaonRoadGraph {

    // =========================================================
    // GRAPH DATA
    // =========================================================

    /*
     * Stores every OSM node.
     *
     * Key:
     *     OSM node ID
     *
     * Value:
     *     GraphNode containing latitude and longitude
     */
    private final Map<String, GraphNode> nodes =
            new HashMap<>();


    /*
     * Adjacency list representation of the road graph.
     *
     * Example:
     *
     * node A
     *   ↓
     * [edge to B, edge to C, edge to D]
     *
     * Each GraphEdge contains:
     * - destination node
     * - distance
     * - travel time
     */
    private final Map<String, List<GraphEdge>> adjacencyList =
            new HashMap<>();


    // =========================================================
    // JSON CONFIGURATION
    // =========================================================

    private final JsonMapper jsonMapper;

    private final String graphPath;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public GurgaonRoadGraph(

            JsonMapper jsonMapper,

            @Value("${ambulance.graph.path}")
            String graphPath

    ) {

        this.jsonMapper =
                jsonMapper;

        this.graphPath =
                graphPath;

        loadGraph();
    }


    // =========================================================
    // LOAD GRAPH
    // =========================================================

    private void loadGraph() {

        long startTime =
                System.currentTimeMillis();


        System.out.println();

        System.out.println(
                "=========================================="
        );

        System.out.println(
                " Loading Gurgaon OSM Road Graph"
        );

        System.out.println(
                "=========================================="
        );


        Path path =
                Path.of(graphPath);


        // -----------------------------------------------------
        // Check graph file
        // -----------------------------------------------------

        if (!Files.exists(path)) {

            throw new IllegalStateException(
                    "Graph file not found: "
                            + path.toAbsolutePath()
            );
        }


        try (
                JsonParser parser =
                        jsonMapper.createParser(
                                Files.newInputStream(path)
                        )
        ) {

            // -------------------------------------------------
            // Root JSON object
            // -------------------------------------------------

            JsonToken token =
                    parser.nextToken();


            if (
                    token != JsonToken.START_OBJECT
            ) {

                throw new IllegalStateException(
                        "Invalid graph JSON. "
                                + "Expected root object."
                );
            }


            // -------------------------------------------------
            // Read root fields
            // -------------------------------------------------

            while (
                    parser.nextToken()
                            != JsonToken.END_OBJECT
            ) {

                String fieldName =
                        parser.currentName();


                parser.nextToken();


                // ---------------------------------------------
                // Nodes
                // ---------------------------------------------

                if ("nodes".equals(fieldName)) {

                    loadNodes(parser);

                }


                // ---------------------------------------------
                // Edges
                // ---------------------------------------------

                else if ("edges".equals(fieldName)) {

                    loadEdges(parser);

                }


                // ---------------------------------------------
                // Ignore unknown fields
                // ---------------------------------------------

                else {

                    parser.skipChildren();
                }
            }


        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to load Gurgaon road graph",
                    exception
            );
        }


        long elapsed =
                System.currentTimeMillis()
                        - startTime;


        long edgeCount =
                adjacencyList.values()
                        .stream()
                        .mapToLong(List::size)
                        .sum();


        // =====================================================
        // GRAPH INFORMATION
        // =====================================================

        System.out.println(
                "Graph nodes loaded: "
                        + nodes.size()
        );


        System.out.println(
                "Graph edges loaded: "
                        + edgeCount
        );


        System.out.println(
                "Graph loading time: "
                        + elapsed
                        + " ms"
        );


        System.out.println(
                "=========================================="
        );

        System.out.println();
    }


    // =========================================================
    // LOAD NODES
    // =========================================================

    private void loadNodes(
            JsonParser parser
    ) throws IOException {

        /*
         * The nodes section must be a JSON object.
         *
         * Example:
         *
         * "nodes": {
         *
         *     "9748049119": {
         *         "latitude": 28.45,
         *         "longitude": 77.03
         *     }
         *
         * }
         */

        if (
                parser.currentToken()
                        != JsonToken.START_OBJECT
        ) {

            throw new IllegalStateException(
                    "Invalid nodes section. "
                            + "Expected JSON object."
            );
        }


        // -----------------------------------------------------
        // Read every OSM node
        // -----------------------------------------------------

        while (
                parser.nextToken()
                        != JsonToken.END_OBJECT
        ) {

            /*
             * The node ID is the JSON property name.
             *
             * Example:
             *
             * "9748049119": {...}
             *
             * currentName() therefore gives:
             *
             * 9748049119
             */

            String nodeId =
                    parser.currentName();


            // -------------------------------------------------
            // Move from property name to node object
            // -------------------------------------------------

            JsonToken nodeToken =
                    parser.nextToken();


            /*
             * Every node must contain a JSON object.
             */

            if (
                    nodeToken
                            != JsonToken.START_OBJECT
            ) {

                parser.skipChildren();

                continue;
            }


            double latitude =
                    0.0;


            double longitude =
                    0.0;


            // -------------------------------------------------
            // Read node properties
            // -------------------------------------------------

            while (
                    parser.nextToken()
                            != JsonToken.END_OBJECT
            ) {

                String fieldName =
                        parser.currentName();


                parser.nextToken();


                // ---------------------------------------------
                // Latitude
                // ---------------------------------------------

                if (
                        "latitude".equals(fieldName)
                ) {

                    latitude =
                            parser.getDoubleValue();
                }


                // ---------------------------------------------
                // Longitude
                // ---------------------------------------------

                else if (
                        "longitude".equals(fieldName)
                ) {

                    longitude =
                            parser.getDoubleValue();
                }


                // ---------------------------------------------
                // Ignore unknown properties
                // ---------------------------------------------

                else {

                    parser.skipChildren();
                }
            }


            // -------------------------------------------------
            // Create GraphNode
            // -------------------------------------------------

            GraphNode node =
                    new GraphNode(

                            nodeId,

                            "OSM-" + nodeId,

                            latitude,

                            longitude

                    );


            // -------------------------------------------------
            // Store node using exact OSM ID
            // -------------------------------------------------

            nodes.put(
                    nodeId,
                    node
            );


            // -------------------------------------------------
            // Create empty adjacency list
            // -------------------------------------------------

            adjacencyList.putIfAbsent(
                    nodeId,
                    new ArrayList<>()
            );
        }


        System.out.println(
                "Finished loading nodes..."
        );


        // =====================================================
        // DEBUG VERIFICATION
        // =====================================================

        /*
         * These checks verify the exact node IDs that were
         * involved in the previous algorithm-comparison error.
         */

        System.out.println(
                "DEBUG: Node 9748049119 exists: "
                        + nodes.containsKey("9748049119")
        );


        System.out.println(
                "DEBUG: Node 9914355731 exists: "
                        + nodes.containsKey("9914355731")
        );
    }


    // =========================================================
    // LOAD EDGES
    // =========================================================

    private void loadEdges(
            JsonParser parser
    ) throws IOException {


        if (
                parser.currentToken()
                        != JsonToken.START_ARRAY
        ) {

            throw new IllegalStateException(
                    "Invalid edges section."
            );
        }


        long edgeCounter =
                0;


        // -----------------------------------------------------
        // Read every edge
        // -----------------------------------------------------

        while (
                parser.nextToken()
                        != JsonToken.END_ARRAY
        ) {

            String from =
                    null;


            String to =
                    null;


            double distanceKm =
                    0.0;


            double travelTimeMinutes =
                    0.0;


            // -------------------------------------------------
            // Every edge must be a JSON object
            // -------------------------------------------------

            if (
                    parser.currentToken()
                            != JsonToken.START_OBJECT
            ) {

                parser.skipChildren();

                continue;
            }


            // -------------------------------------------------
            // Read edge properties
            // -------------------------------------------------

            while (
                    parser.nextToken()
                            != JsonToken.END_OBJECT
            ) {

                String fieldName =
                        parser.currentName();


                parser.nextToken();


                switch (fieldName) {

                    case "from":

                        from =
                                parser.getValueAsString();

                        break;


                    case "to":

                        to =
                                parser.getValueAsString();

                        break;


                    case "distanceKm":

                        distanceKm =
                                parser.getDoubleValue();

                        break;


                    case "travelTimeMinutes":

                        travelTimeMinutes =
                                parser.getDoubleValue();

                        break;


                    default:

                        parser.skipChildren();
                }
            }


            // -------------------------------------------------
            // Validate edge
            // -------------------------------------------------

            if (
                    from == null
                            || to == null
            ) {

                continue;
            }


            // -------------------------------------------------
            // Both nodes must exist
            // -------------------------------------------------

            if (
                    !nodes.containsKey(from)
                            || !nodes.containsKey(to)
            ) {

                continue;
            }


            // -------------------------------------------------
            // Create edge
            // -------------------------------------------------

            GraphEdge edge =
                    new GraphEdge(

                            to,

                            distanceKm,

                            travelTimeMinutes

                    );


            // -------------------------------------------------
            // Add edge to adjacency list
            // -------------------------------------------------

            adjacencyList
                    .get(from)
                    .add(edge);


            edgeCounter++;


            // -------------------------------------------------
            // Progress information
            // -------------------------------------------------

            if (
                    edgeCounter % 100000 == 0
            ) {

                System.out.println(
                        "Loaded "
                                + edgeCounter
                                + " edges..."
                );
            }
        }


        System.out.println(
                "Finished loading edges..."
        );
    }


    // =========================================================
    // GET NODE
    // =========================================================

    /*
     * Returns a graph node using its OSM node ID.
     *
     * Example:
     *
     * getNode("2225581300")
     *
     */

    public GraphNode getNode(String nodeId) {

        if (nodeId == null) {
            return null;
        }

        GraphNode node =
                nodes.get(nodeId);

        if (node == null) {

            System.out.println(
                    "DEBUG: Node not found: ["
                            + nodeId
                            + "]"
            );

            System.out.println(
                    "DEBUG: Graph contains exact key: "
                            + nodes.containsKey(nodeId)
            );

            System.out.println(
                    "DEBUG: Graph node count: "
                            + nodes.size()
            );
        }

        return node;
    }


    // =========================================================
    // GET NEIGHBORS
    // =========================================================

    /*
     * Returns every outgoing road from a node.
     *
     * Dijkstra and A* use this method to traverse the graph.
     */

    public List<GraphEdge> getNeighbors(
            String nodeId
    ) {

        return adjacencyList.getOrDefault(
                nodeId,
                List.of()
        );
    }


    // =========================================================
    // GET ALL NODES
    // =========================================================

    /*
     * Used for:
     *
     * - nearest-node lookup
     * - debugging
     * - graph statistics
     */

    public Map<String, GraphNode> getNodes() {

        return Map.copyOf(nodes);
    }


    // =========================================================
    // FIND NEAREST OSM NODE
    // =========================================================

    /*
     * Converts a real-world GPS coordinate into the nearest
     * node in our OSM road graph.
     *
     * This is NOT the routing algorithm.
     *
     * It only performs:
     *
     * GPS coordinates
     *       ↓
     * nearest OSM graph node
     *
     * After that, Dijkstra or A* performs the actual
     * route optimization.
     */

    public GraphNode findNearestNode(

            double latitude,

            double longitude

    ) {

        GraphNode nearestNode =
                null;


        double shortestDistance =
                Double.POSITIVE_INFINITY;


        // -----------------------------------------------------
        // Check every OSM node
        // -----------------------------------------------------

        for (
                GraphNode node :
                nodes.values()
        ) {

            double distance =
                    haversineDistance(

                            latitude,
                            longitude,

                            node.latitude(),
                            node.longitude()

                    );


            if (
                    distance < shortestDistance
            ) {

                shortestDistance =
                        distance;

                nearestNode =
                        node;
            }
        }


        // -----------------------------------------------------
        // Safety check
        // -----------------------------------------------------

        if (nearestNode == null) {

            throw new IllegalStateException(
                    "Road graph contains no nodes"
            );
        }


        return nearestNode;
    }


    // =========================================================
    // HAVERSINE DISTANCE
    // =========================================================

    /*
     * Calculates straight-line geographic distance.
     *
     * IMPORTANT:
     *
     * Haversine is used ONLY for finding the closest graph
     * node.
     *
     * It is NOT used to determine the optimal ambulance route.
     *
     * Dijkstra and A* calculate the actual road-network route.
     */

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
                2 * Math.atan2(

                        Math.sqrt(a),

                        Math.sqrt(1 - a)

                );


        return EARTH_RADIUS_KM * c;
    }
}