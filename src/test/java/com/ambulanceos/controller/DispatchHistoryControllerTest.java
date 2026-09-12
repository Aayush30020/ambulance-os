package com.ambulanceos.controller;

import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.exception.DispatchNotFoundException;
import com.ambulanceos.exception.GlobalExceptionHandler;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.service.DispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DispatchHistoryControllerTest {

    @Mock
    private DispatchRepository dispatchRepository;

    @Mock
    private DispatchService dispatchService;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {

        DispatchHistoryController controller =
                new DispatchHistoryController(
                        dispatchRepository,
                        dispatchService
                );

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .setControllerAdvice(
                                new GlobalExceptionHandler()
                        )
                        .build();
    }


    // =========================================================
    // TEST 1
    // =========================================================
    //
    // GET /api/dispatches
    //
    // Should return dispatch history ordered by dispatchedAt.
    //
    // =========================================================

    @Test
    void shouldGetAllDispatches() throws Exception {

        Dispatch firstDispatch =
                createDispatch(
                        101L,
                        1L,
                        201L,
                        "AMB-101",
                        "Medanta Hospital",
                        "COMPLETED"
                );

        Dispatch secondDispatch =
                createDispatch(
                        102L,
                        2L,
                        202L,
                        "AMB-102",
                        "Artemis Hospital",
                        "IN_PROGRESS"
                );


        when(
                dispatchRepository
                        .findAllByOrderByDispatchedAtDesc()
        ).thenReturn(
                List.of(
                        firstDispatch,
                        secondDispatch
                )
        );


        mockMvc.perform(
                        get("/api/dispatches")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$[0].emergencyId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].ambulanceId")
                                .value(201)
                )
                .andExpect(
                        jsonPath("$[0].ambulanceNumber")
                                .value("AMB-101")
                )
                .andExpect(
                        jsonPath("$[0].hospitalName")
                                .value("Medanta Hospital")
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("COMPLETED")
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(102)
                )
                .andExpect(
                        jsonPath("$[1].ambulanceNumber")
                                .value("AMB-102")
                )
                .andExpect(
                        jsonPath("$[1].status")
                                .value("IN_PROGRESS")
                );


        verify(
                dispatchRepository
        ).findAllByOrderByDispatchedAtDesc();
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // GET /api/dispatches/{id}
    //
    // Existing dispatch should be returned successfully.
    //
    // =========================================================

    @Test
    void shouldGetDispatchById() throws Exception {

        Dispatch dispatch =
                createDispatch(
                        101L,
                        1L,
                        201L,
                        "AMB-101",
                        "Medanta Hospital",
                        "IN_PROGRESS"
                );


        when(
                dispatchRepository.findById(101L)
        ).thenReturn(
                Optional.of(dispatch)
        );


        mockMvc.perform(
                        get("/api/dispatches/101")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.emergencyId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.ambulanceId")
                                .value(201)
                )
                .andExpect(
                        jsonPath("$.ambulanceNumber")
                                .value("AMB-101")
                )
                .andExpect(
                        jsonPath("$.hospitalName")
                                .value("Medanta Hospital")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("IN_PROGRESS")
                );


        verify(
                dispatchRepository
        ).findById(101L);
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // GET /api/dispatches/{id}
    //
    // Missing dispatch should return:
    //
    // HTTP 404
    // DISPATCH_NOT_FOUND
    //
    // =========================================================

    @Test
    void shouldReturn404WhenDispatchDoesNotExist()
            throws Exception {

        when(
                dispatchRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );


        mockMvc.perform(
                        get("/api/dispatches/999")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("DISPATCH_NOT_FOUND")
                );


        verify(
                dispatchRepository
        ).findById(999L);
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // PUT /api/dispatches/{id}/complete
    //
    // Should delegate completion to DispatchService.
    //
    // =========================================================

    @Test
    void shouldCompleteDispatch() throws Exception {

        Dispatch completedDispatch =
                createDispatch(
                        101L,
                        1L,
                        201L,
                        "AMB-101",
                        "Medanta Hospital",
                        "COMPLETED"
                );


        when(
                dispatchService.completeDispatch(101L)
        ).thenReturn(
                completedDispatch
        );


        mockMvc.perform(
                        put("/api/dispatches/101/complete")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.emergencyId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.ambulanceId")
                                .value(201)
                )
                .andExpect(
                        jsonPath("$.ambulanceNumber")
                                .value("AMB-101")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                );


        verify(
                dispatchService
        ).completeDispatch(101L);
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // PUT /api/dispatches/{id}/complete
    //
    // If the dispatch does not exist, DispatchService throws
    // DispatchNotFoundException.
    //
    // GlobalExceptionHandler should return HTTP 404.
    //
    // =========================================================

    @Test
    void shouldReturn404WhenCompletingMissingDispatch()
            throws Exception {

        when(
                dispatchService.completeDispatch(999L)
        ).thenThrow(
                new DispatchNotFoundException(999L)
        );


        mockMvc.perform(
                        put("/api/dispatches/999/complete")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("DISPATCH_NOT_FOUND")
                );


        verify(
                dispatchService
        ).completeDispatch(999L);
    }


    // =========================================================
    // HELPER METHOD
    // =========================================================
    //
    // Creates a Dispatch entity used by the controller tests.
    //
    // =========================================================

    private Dispatch createDispatch(
            Long id,
            Long emergencyId,
            Long ambulanceId,
            String ambulanceNumber,
            String hospitalName,
            String status
    ) {

        return Dispatch.builder()

                .id(id)

                .emergencyId(
                        emergencyId
                )

                .ambulanceId(
                        ambulanceId
                )

                .ambulanceNumber(
                        ambulanceNumber
                )

                .hospitalId(
                        301L
                )

                .hospitalName(
                        hospitalName
                )

                .routingAlgorithm(
                        "DIJKSTRA"
                )

                .distanceToEmergencyKm(
                        4.25
                )

                .timeToEmergencyMinutes(
                        8.50
                )

                .distanceToHospitalKm(
                        5.75
                )

                .timeToHospitalMinutes(
                        11.25
                )

                .totalDistanceKm(
                        10.00
                )

                .totalEstimatedTimeMinutes(
                        19.75
                )

                .status(
                        status
                )

                .dispatchedAt(
                        LocalDateTime.of(
                                2026,
                                9,
                                13,
                                10,
                                30
                        )
                )

                .completedAt(
                        "COMPLETED".equals(status)
                                ? LocalDateTime.of(
                                2026,
                                9,
                                13,
                                11,
                                0
                        )
                                : null
                )

                .build();
    }
}