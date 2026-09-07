package com.ambulanceos.graph;

public record GraphNode(
        String id,
        String name,
        double latitude,
        double longitude
) {
}