package com.ambulanceos.dto;

import java.util.List;

public record DispatchPlanResponse(

        Long emergencyId,

        AmbulanceDetails ambulance,

        HospitalDetails hospital,

        RouteDetails route

) {

    public record AmbulanceDetails(

            Long id,

            String ambulanceNumber,

            String type,

            String status,

            double distanceKm

    ) {
    }


    public record HospitalDetails(

            Long id,

            String hospitalCode,

            String name,

            String facilityType,

            Integer availableBeds,

            double distanceKm

    ) {
    }


    public record RouteDetails(

            String sourceNode,

            String destinationNode,

            double distanceKm,

            List<String> nodes

    ) {
    }
}