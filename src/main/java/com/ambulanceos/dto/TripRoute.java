package com.ambulanceos.dto;

import java.util.List;

public record TripRoute(

        TripStatus status,

        RouteSegment routeToEmergency,

        RouteSegment routeToHospital

) {

    public enum TripStatus {

        TO_EMERGENCY,

        AT_EMERGENCY,

        TO_HOSPITAL,

        COMPLETED

    }


    public record RouteSegment(

            double distanceKm,

            double estimatedTravelTimeMinutes,

            String trafficLevel,

            List<String> nodePath,

            List<RoutePoint> coordinates

    ) {
    }


    public record RoutePoint(

            double latitude,

            double longitude

    ) {
    }
}