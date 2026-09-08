package com.ambulanceos.dto;

import java.util.List;

public record TrafficDijkstraResponse(

        String sourceNode,

        String destinationNode,

        double distanceKm,

        double estimatedTravelTimeMinutes,

        String trafficLevel,

        List<String> path

) {
}