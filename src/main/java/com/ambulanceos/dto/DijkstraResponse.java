package com.ambulanceos.dto;

import java.util.List;

public record DijkstraResponse(

        String sourceNode,

        String destinationNode,

        double distanceKm,

        List<String> path

) {
}