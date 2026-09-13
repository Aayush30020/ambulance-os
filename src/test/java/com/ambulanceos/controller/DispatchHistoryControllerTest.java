package com.ambulanceos.controller;

import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.exception.DispatchNotFoundException;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.service.DispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchHistoryControllerTest {

    @Mock
    private DispatchRepository dispatchRepository;

    @Mock
    private DispatchService dispatchService;

    @InjectMocks
    private DispatchHistoryController dispatchHistoryController;


    // =========================================================
    // GET ALL DISPATCHES
    // =========================================================

    @Test
    void shouldGetAllDispatches() {

        Dispatch dispatch1 =
                Dispatch.builder()
                        .id(1L)
                        .emergencyId(101L)
                        .ambulanceId(201L)
                        .ambulanceNumber("AMB-101")
                        .hospitalId(301L)
                        .hospitalName("Medanta Hospital")
                        .routingAlgorithm("DIJKSTRA")
                        .distanceToEmergencyKm(5.0)
                        .timeToEmergencyMinutes(7.5)
                        .distanceToHospitalKm(8.0)
                        .timeToHospitalMinutes(12.0)
                        .totalDistanceKm(13.0)
                        .totalEstimatedTimeMinutes(19.5)
                        .status("COMPLETED")
                        .build();

        Dispatch dispatch2 =
                Dispatch.builder()
                        .id(2L)
                        .emergencyId(102L)
                        .ambulanceId(202L)
                        .ambulanceNumber("AMB-102")
                        .hospitalId(302L)
                        .hospitalName("Artemis Hospital")
                        .routingAlgorithm("ASTAR")
                        .distanceToEmergencyKm(4.0)
                        .timeToEmergencyMinutes(6.0)
                        .distanceToHospitalKm(7.0)
                        .timeToHospitalMinutes(10.5)
                        .totalDistanceKm(11.0)
                        .totalEstimatedTimeMinutes(16.5)
                        .status("IN_PROGRESS")
                        .build();


        List<Dispatch> dispatches =
                List.of(
                        dispatch2,
                        dispatch1
                );


        when(
                dispatchRepository
                        .findAllByOrderByDispatchedAtDesc()
        ).thenReturn(dispatches);


        List<Dispatch> result =
                dispatchHistoryController
                        .getAllDispatches();


        assertThat(result)
                .hasSize(2)
                .containsExactly(
                        dispatch2,
                        dispatch1
                );

        verify(
                dispatchRepository
        ).findAllByOrderByDispatchedAtDesc();
    }


    // =========================================================
    // GET DISPATCH BY ID
    // =========================================================

    @Test
    void shouldGetDispatchById() {

        Long dispatchId = 1L;

        Dispatch dispatch =
                Dispatch.builder()
                        .id(dispatchId)
                        .emergencyId(101L)
                        .ambulanceId(201L)
                        .ambulanceNumber("AMB-101")
                        .hospitalId(301L)
                        .hospitalName("Medanta Hospital")
                        .routingAlgorithm("DIJKSTRA")
                        .distanceToEmergencyKm(5.0)
                        .timeToEmergencyMinutes(7.5)
                        .distanceToHospitalKm(8.0)
                        .timeToHospitalMinutes(12.0)
                        .totalDistanceKm(13.0)
                        .totalEstimatedTimeMinutes(19.5)
                        .status("IN_PROGRESS")
                        .build();


        when(
                dispatchRepository.findById(
                        dispatchId
                )
        ).thenReturn(
                Optional.of(dispatch)
        );


        Dispatch result =
                dispatchHistoryController
                        .getDispatchById(
                                dispatchId
                        );


        assertThat(result)
                .isEqualTo(dispatch);

        verify(
                dispatchRepository
        ).findById(
                dispatchId
        );
    }


    // =========================================================
    // DISPATCH NOT FOUND
    // =========================================================

    @Test
    void shouldThrowExceptionWhenDispatchDoesNotExist() {

        Long dispatchId = 999L;


        when(
                dispatchRepository.findById(
                        dispatchId
                )
        ).thenReturn(
                Optional.empty()
        );


        assertThatThrownBy(() ->
                dispatchHistoryController
                        .getDispatchById(
                                dispatchId
                        )
        )
                .isInstanceOf(
                        DispatchNotFoundException.class
                );


        verify(
                dispatchRepository
        ).findById(
                dispatchId
        );
    }


    // =========================================================
    // COMPLETE DISPATCH
    // =========================================================

    @Test
    void shouldCompleteDispatch() {

        Long dispatchId = 1L;

        Dispatch dispatch =
                Dispatch.builder()
                        .id(dispatchId)
                        .emergencyId(101L)
                        .ambulanceId(201L)
                        .ambulanceNumber("AMB-101")
                        .hospitalId(301L)
                        .hospitalName("Medanta Hospital")
                        .routingAlgorithm("DIJKSTRA")
                        .distanceToEmergencyKm(5.0)
                        .timeToEmergencyMinutes(7.5)
                        .distanceToHospitalKm(8.0)
                        .timeToHospitalMinutes(12.0)
                        .totalDistanceKm(13.0)
                        .totalEstimatedTimeMinutes(19.5)
                        .status("COMPLETED")
                        .build();


        when(
                dispatchService.completeDispatch(
                        dispatchId
                )
        ).thenReturn(dispatch);


        Dispatch result =
                dispatchHistoryController
                        .completeDispatch(
                                dispatchId
                        );


        assertThat(result)
                .isEqualTo(dispatch);

        verify(
                dispatchService
        ).completeDispatch(
                dispatchId
        );
    }
}