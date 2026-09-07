package com.ambulanceos.graph;

public record GraphEdge(
        String destinationNodeId,
        double distanceKm,
        double travelTimeMinutes
) {
}