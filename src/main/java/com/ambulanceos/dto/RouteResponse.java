package com.ambulanceos.dto;

import java.util.List;

public record RouteResponse(

        Long ambulanceId,

        String ambulanceNumber,

        Long hospitalId,

        String hospitalName,

        String sourceNode,

        String destinationNode,

        double distanceKm,

        double estimatedTravelTimeMinutes,

        String trafficLevel,

        List<String> nodePath,

        List<RoutePoint> route

) {

    public record RoutePoint(

            double latitude,

            double longitude

    ) {
    }
}