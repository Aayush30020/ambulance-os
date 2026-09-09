package com.ambulanceos.dto;

import java.util.List;

public record DispatchPlanResponse(

        Long dispatchId,

        Long emergencyId,

        AmbulanceDetails ambulance,

        HospitalDetails hospital,

        RouteDetails route,

        TripRoute trip

) {

    public record AmbulanceDetails(

            Long id,

            String ambulanceNumber,

            String type,

            String status,

            double distanceKm,

            double estimatedTravelTimeMinutes,

            String trafficLevel

    ) {
    }

    public record HospitalDetails(

            Long id,

            String hospitalCode,

            String name,

            String facilityType,

            Integer availableBeds,

            double distanceKm,

            double estimatedTravelTimeMinutes,

            String trafficLevel

    ) {
    }

    public record RouteDetails(

            String sourceNode,

            String destinationNode,

            double distanceKm,

            double estimatedTravelTimeMinutes,

            String trafficLevel,

            List<String> nodes

    ) {
    }
}