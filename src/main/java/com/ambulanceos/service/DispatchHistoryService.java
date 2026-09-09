package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchHistoryResponse;
import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.repository.DispatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DispatchHistoryService {

    private final DispatchRepository dispatchRepository;


    // =========================================================
    // CREATE DISPATCH HISTORY RECORD
    // =========================================================

    public Dispatch createDispatch(
            Long emergencyId,
            DispatchResponse ambulanceResponse,
            HospitalSelectionResponse hospitalResponse,
            String hospitalName
    ) {

        double distanceToEmergency =
                ambulanceResponse.distanceKm();

        double timeToEmergency =
                ambulanceResponse.estimatedTravelTimeMinutes();

        double distanceToHospital =
                hospitalResponse.distanceKm();

        double timeToHospital =
                hospitalResponse.estimatedTravelTimeMinutes();

        double totalDistance =
                distanceToEmergency
                        + distanceToHospital;

        double totalEstimatedTime =
                timeToEmergency
                        + timeToHospital;


        Dispatch dispatch =
                Dispatch.builder()

                        .emergencyId(
                                emergencyId
                        )

                        .ambulanceId(
                                ambulanceResponse.ambulanceId()
                        )

                        .ambulanceNumber(
                                ambulanceResponse.ambulanceNumber()
                        )

                        .hospitalId(
                                hospitalResponse.hospitalId()
                        )

                        .hospitalName(
                                hospitalName
                        )

                        .routingAlgorithm(
                                "TRAFFIC_DIJKSTRA"
                        )

                        .distanceToEmergencyKm(
                                distanceToEmergency
                        )

                        .timeToEmergencyMinutes(
                                timeToEmergency
                        )

                        .distanceToHospitalKm(
                                distanceToHospital
                        )

                        .timeToHospitalMinutes(
                                timeToHospital
                        )

                        .totalDistanceKm(
                                totalDistance
                        )

                        .totalEstimatedTimeMinutes(
                                totalEstimatedTime
                        )

                        .status(
                                "IN_PROGRESS"
                        )

                        .dispatchedAt(
                                LocalDateTime.now()
                        )

                        .build();


        return dispatchRepository.save(
                dispatch
        );
    }


    // =========================================================
    // COMPLETE DISPATCH
    // =========================================================

    public DispatchHistoryResponse completeDispatch(
            Long dispatchId
    ) {

        Dispatch dispatch =
                dispatchRepository.findById(
                        dispatchId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Dispatch not found with id: "
                                        + dispatchId
                        )
                );


        dispatch.setStatus(
                "COMPLETED"
        );

        dispatch.setCompletedAt(
                LocalDateTime.now()
        );


        Dispatch savedDispatch =
                dispatchRepository.save(
                        dispatch
                );


        return mapToResponse(
                savedDispatch
        );
    }


    // =========================================================
    // GET ALL HISTORY
    // =========================================================

    public List<DispatchHistoryResponse>
    getAllDispatches() {

        return dispatchRepository
                .findAllByOrderByDispatchedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET SINGLE DISPATCH
    // =========================================================

    public DispatchHistoryResponse
    getDispatchById(
            Long id
    ) {

        Dispatch dispatch =
                dispatchRepository.findById(
                        id
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Dispatch not found with id: "
                                        + id
                        )
                );


        return mapToResponse(
                dispatch
        );
    }


    // =========================================================
    // MAP ENTITY → RESPONSE
    // =========================================================

    private DispatchHistoryResponse mapToResponse(
            Dispatch dispatch
    ) {

        return new DispatchHistoryResponse(

                dispatch.getId(),

                dispatch.getEmergencyId(),

                dispatch.getAmbulanceId(),

                dispatch.getAmbulanceNumber(),

                dispatch.getHospitalId(),

                dispatch.getHospitalName(),

                dispatch.getRoutingAlgorithm(),

                dispatch.getDistanceToEmergencyKm(),

                dispatch.getTimeToEmergencyMinutes(),

                dispatch.getDistanceToHospitalKm(),

                dispatch.getTimeToHospitalMinutes(),

                dispatch.getTotalDistanceKm(),

                dispatch.getTotalEstimatedTimeMinutes(),

                dispatch.getStatus(),

                dispatch.getDispatchedAt(),

                dispatch.getCompletedAt()
        );
    }
}