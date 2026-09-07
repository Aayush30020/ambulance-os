package com.ambulanceos.graph;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GurgaonRoadGraph {

    private final Map<String, GraphNode> nodes =
            new HashMap<>();

    private final Map<String, List<GraphEdge>> adjacencyList =
            new HashMap<>();

    public GurgaonRoadGraph() {

        createNodes();

        createEdges();
    }

    private void createNodes() {

        addNode(
                new GraphNode(
                        "G01",
                        "Sector 15",
                        28.4487,
                        77.0384
                )
        );

        addNode(
                new GraphNode(
                        "G02",
                        "Sector 14",
                        28.4590,
                        77.0300
                )
        );

        addNode(
                new GraphNode(
                        "G03",
                        "IFFCO Chowk",
                        28.4692,
                        77.0720
                )
        );

        addNode(
                new GraphNode(
                        "G04",
                        "MG Road",
                        28.4800,
                        77.0890
                )
        );

        addNode(
                new GraphNode(
                        "G05",
                        "Sikanderpur",
                        28.4810,
                        77.0940
                )
        );

        addNode(
                new GraphNode(
                        "G06",
                        "Golf Course Road",
                        28.4600,
                        77.0900
                )
        );

        addNode(
                new GraphNode(
                        "G07",
                        "Huda City Centre",
                        28.4595,
                        77.0266
                )
        );

        addNode(
                new GraphNode(
                        "G08",
                        "Sector 29",
                        28.4660,
                        77.0430
                )
        );

        addNode(
                new GraphNode(
                        "G09",
                        "Sector 31",
                        28.4520,
                        77.0480
                )
        );

        addNode(
                new GraphNode(
                        "G10",
                        "Medanta",
                        28.4246,
                        76.9957
                )
        );

        addNode(
                new GraphNode(
                        "G11",
                        "Artemis",
                        28.4352,
                        77.0817
                )
        );

        addNode(
                new GraphNode(
                        "G12",
                        "Fortis",
                        28.4597,
                        77.0726
                )
        );
    }

    private void createEdges() {

        addBidirectionalEdge(
                "G01",
                "G02",
                1.4,
                4.0
        );

        addBidirectionalEdge(
                "G01",
                "G09",
                1.1,
                3.0
        );

        addBidirectionalEdge(
                "G02",
                "G07",
                1.2,
                3.0
        );

        addBidirectionalEdge(
                "G02",
                "G08",
                1.5,
                4.0
        );

        addBidirectionalEdge(
                "G07",
                "G08",
                1.4,
                4.0
        );

        addBidirectionalEdge(
                "G07",
                "G09",
                1.3,
                4.0
        );

        addBidirectionalEdge(
                "G08",
                "G03",
                2.0,
                5.0
        );

        addBidirectionalEdge(
                "G08",
                "G12",
                1.6,
                4.0
        );

        addBidirectionalEdge(
                "G09",
                "G12",
                2.1,
                5.0
        );

        addBidirectionalEdge(
                "G03",
                "G04",
                1.8,
                4.0
        );

        addBidirectionalEdge(
                "G03",
                "G12",
                1.1,
                3.0
        );

        addBidirectionalEdge(
                "G04",
                "G05",
                0.8,
                2.0
        );

        addBidirectionalEdge(
                "G05",
                "G06",
                1.5,
                4.0
        );

        addBidirectionalEdge(
                "G06",
                "G11",
                1.9,
                5.0
        );

        addBidirectionalEdge(
                "G06",
                "G12",
                1.7,
                4.0
        );

        addBidirectionalEdge(
                "G09",
                "G10",
                3.2,
                8.0
        );

        addBidirectionalEdge(
                "G07",
                "G10",
                3.5,
                9.0
        );

        addBidirectionalEdge(
                "G11",
                "G04",
                2.2,
                5.0
        );
    }

    private void addNode(
            GraphNode node
    ) {

        nodes.put(
                node.id(),
                node
        );

        adjacencyList.put(
                node.id(),
                new ArrayList<>()
        );
    }

    private void addBidirectionalEdge(
            String sourceNodeId,
            String destinationNodeId,
            double distanceKm,
            double travelTimeMinutes
    ) {

        adjacencyList
                .get(sourceNodeId)
                .add(
                        new GraphEdge(
                                destinationNodeId,
                                distanceKm,
                                travelTimeMinutes
                        )
                );

        adjacencyList
                .get(destinationNodeId)
                .add(
                        new GraphEdge(
                                sourceNodeId,
                                distanceKm,
                                travelTimeMinutes
                        )
                );
    }

    public GraphNode getNode(
            String nodeId
    ) {

        return nodes.get(nodeId);
    }

    public List<GraphEdge> getNeighbors(
            String nodeId
    ) {

        return adjacencyList.getOrDefault(
                nodeId,
                List.of()
        );
    }

    public Map<String, GraphNode> getNodes() {

        return Map.copyOf(nodes);
    }
}