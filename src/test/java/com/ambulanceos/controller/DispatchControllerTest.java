package com.ambulanceos.controller;

import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.exception.GlobalExceptionHandler;
import com.ambulanceos.exception.NoAvailableAmbulanceException;
import com.ambulanceos.service.DispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DispatchControllerTest {

    @Mock
    private DispatchService dispatchService;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {

        DispatchController controller =
                new DispatchController(
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
    // GET /api/dispatch/ambulance/{emergencyId}
    //
    // Should return the best available ambulance without
    // changing its status.
    //
    // =========================================================

    @Test
    void shouldFindBestAmbulance() throws Exception {

        DispatchResponse response =
                new DispatchResponse(
                        101L,
                        201L,
                        "AMB-101",
                        "ALS",
                        "AVAILABLE",
                        4.25,
                        8.50,
                        "MODERATE"
                );


        when(
                dispatchService.findBestAmbulance(101L)
        ).thenReturn(response);


        mockMvc.perform(
                        get(
                                "/api/dispatch/ambulance/101"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.emergencyId")
                                .value(101)
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
                        jsonPath("$.ambulanceType")
                                .value("ALS")
                )
                .andExpect(
                        jsonPath("$.ambulanceStatus")
                                .value("AVAILABLE")
                )
                .andExpect(
                        jsonPath("$.distanceKm")
                                .value(4.25)
                )
                .andExpect(
                        jsonPath("$.estimatedTravelTimeMinutes")
                                .value(8.50)
                )
                .andExpect(
                        jsonPath("$.trafficLevel")
                                .value("MODERATE")
                );


        verify(
                dispatchService
        ).findBestAmbulance(101L);
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // POST /api/dispatch/{emergencyId}
    //
    // Should dispatch the selected ambulance.
    //
    // =========================================================

    @Test
    void shouldDispatchAmbulance() throws Exception {

        DispatchResponse response =
                new DispatchResponse(
                        101L,
                        201L,
                        "AMB-101",
                        "ALS",
                        "EN_ROUTE",
                        4.25,
                        8.50,
                        "MODERATE"
                );


        when(
                dispatchService.dispatchAmbulance(101L)
        ).thenReturn(response);


        mockMvc.perform(
                        post("/api/dispatch/101")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.emergencyId")
                                .value(101)
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
                        jsonPath("$.ambulanceType")
                                .value("ALS")
                )
                .andExpect(
                        jsonPath("$.ambulanceStatus")
                                .value("EN_ROUTE")
                )
                .andExpect(
                        jsonPath("$.distanceKm")
                                .value(4.25)
                )
                .andExpect(
                        jsonPath("$.estimatedTravelTimeMinutes")
                                .value(8.50)
                )
                .andExpect(
                        jsonPath("$.trafficLevel")
                                .value("MODERATE")
                );


        verify(
                dispatchService
        ).dispatchAmbulance(101L);
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // If there is no available ambulance, the service throws
    // NoAvailableAmbulanceException.
    //
    // GlobalExceptionHandler should convert this to:
    //
    // HTTP 409 CONFLICT
    //
    // =========================================================

    @Test
    void shouldReturn409WhenNoAmbulanceIsAvailable()
            throws Exception {

        when(
                dispatchService.findBestAmbulance(101L)
        ).thenThrow(
                new NoAvailableAmbulanceException(
                        "No available ambulance found"
                )
        );


        mockMvc.perform(
                        get(
                                "/api/dispatch/ambulance/101"
                        )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "NO_AVAILABLE_AMBULANCE"
                                )
                );
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // The same business error should also be handled when
    // attempting an actual dispatch.
    //
    // =========================================================

    @Test
    void shouldReturn409WhenDispatchHasNoAvailableAmbulance()
            throws Exception {

        when(
                dispatchService.dispatchAmbulance(101L)
        ).thenThrow(
                new NoAvailableAmbulanceException(
                        "No available ambulance found"
                )
        );


        mockMvc.perform(
                        post("/api/dispatch/101")
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "NO_AVAILABLE_AMBULANCE"
                                )
                );
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // Path variable should be correctly converted from the URL
    // into Long before reaching the service.
    //
    // =========================================================

    @Test
    void shouldPassCorrectEmergencyIdToService()
            throws Exception {

        DispatchResponse response =
                new DispatchResponse(
                        500L,
                        301L,
                        "AMB-103",
                        "BLS",
                        "AVAILABLE",
                        2.75,
                        5.25,
                        "LOW"
                );


        when(
                dispatchService.findBestAmbulance(500L)
        ).thenReturn(response);


        mockMvc.perform(
                        get(
                                "/api/dispatch/ambulance/500"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.emergencyId")
                                .value(500)
                );


        verify(
                dispatchService
        ).findBestAmbulance(500L);
    }
}