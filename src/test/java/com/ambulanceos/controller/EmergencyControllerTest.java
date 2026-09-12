package com.ambulanceos.controller;

import com.ambulanceos.dto.EmergencyRequest;
import com.ambulanceos.dto.EmergencyResponse;
import com.ambulanceos.exception.GlobalExceptionHandler;
import com.ambulanceos.service.EmergencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmergencyControllerTest {

    @Mock
    private EmergencyService emergencyService;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {

        EmergencyController controller =
                new EmergencyController(
                        emergencyService
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
    // POST /api/emergencies
    //
    // Valid request should create an emergency and return
    // HTTP 201 CREATED.
    //
    // =========================================================

    @Test
    void shouldCreateEmergency() throws Exception {

        EmergencyResponse response =
                new EmergencyResponse(
                        101L,
                        "Golf Course Road",
                        28.4595,
                        77.0266,
                        "HIGH",
                        "TRAUMA",
                        "Road accident",
                        "ACTIVE",
                        LocalDateTime.of(
                                2026,
                                9,
                                13,
                                10,
                                30
                        )
                );


        when(
                emergencyService.createEmergency(
                        any(EmergencyRequest.class)
                )
        ).thenReturn(response);


        String requestJson = """
                {
                    "location": "Golf Course Road",
                    "latitude": 28.4595,
                    "longitude": 77.0266,
                    "priority": "HIGH",
                    "facility": "TRAUMA",
                    "notes": "Road accident"
                }
                """;


        mockMvc.perform(
                        post("/api/emergencies")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestJson)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.location")
                                .value("Golf Course Road")
                )
                .andExpect(
                        jsonPath("$.priority")
                                .value("HIGH")
                )
                .andExpect(
                        jsonPath("$.facility")
                                .value("TRAUMA")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );


        verify(
                emergencyService
        ).createEmergency(
                any(EmergencyRequest.class)
        );
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // GET /api/emergencies
    //
    // Should return all emergencies with HTTP 200.
    //
    // =========================================================

    @Test
    void shouldGetAllEmergencies() throws Exception {

        EmergencyResponse firstEmergency =
                new EmergencyResponse(
                        101L,
                        "Golf Course Road",
                        28.4595,
                        77.0266,
                        "HIGH",
                        "TRAUMA",
                        "Road accident",
                        "ACTIVE",
                        LocalDateTime.of(
                                2026,
                                9,
                                13,
                                10,
                                30
                        )
                );


        EmergencyResponse secondEmergency =
                new EmergencyResponse(
                        102L,
                        "MG Road",
                        28.4700,
                        77.0300,
                        "MEDIUM",
                        "GENERAL",
                        "Medical emergency",
                        "RESPONDING",
                        LocalDateTime.of(
                                2026,
                                9,
                                13,
                                11,
                                0
                        )
                );


        when(
                emergencyService.getAllEmergencies()
        ).thenReturn(
                List.of(
                        firstEmergency,
                        secondEmergency
                )
        );


        mockMvc.perform(
                        get("/api/emergencies")
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
                        jsonPath("$[0].location")
                                .value("Golf Course Road")
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(102)
                )
                .andExpect(
                        jsonPath("$[1].location")
                                .value("MG Road")
                )
                .andExpect(
                        jsonPath("$[1].status")
                                .value("RESPONDING")
                );


        verify(
                emergencyService
        ).getAllEmergencies();
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // GET /api/emergencies/{id}
    //
    // Should return the requested emergency.
    //
    // =========================================================

    @Test
    void shouldGetEmergencyById() throws Exception {

        EmergencyResponse response =
                new EmergencyResponse(
                        101L,
                        "Golf Course Road",
                        28.4595,
                        77.0266,
                        "HIGH",
                        "TRAUMA",
                        "Road accident",
                        "ACTIVE",
                        LocalDateTime.of(
                                2026,
                                9,
                                13,
                                10,
                                30
                        )
                );


        when(
                emergencyService.getEmergencyById(101L)
        ).thenReturn(response);


        mockMvc.perform(
                        get("/api/emergencies/101")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.location")
                                .value("Golf Course Road")
                )
                .andExpect(
                        jsonPath("$.latitude")
                                .value(28.4595)
                )
                .andExpect(
                        jsonPath("$.longitude")
                                .value(77.0266)
                )
                .andExpect(
                        jsonPath("$.priority")
                                .value("HIGH")
                )
                .andExpect(
                        jsonPath("$.facility")
                                .value("TRAUMA")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                );


        verify(
                emergencyService
        ).getEmergencyById(101L);
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // PUT /api/emergencies/{id}/status?status=RESPONDING
    //
    // Should update the emergency status.
    //
    // =========================================================

    @Test
    void shouldUpdateEmergencyStatus() throws Exception {

        EmergencyResponse response =
                new EmergencyResponse(
                        101L,
                        "Golf Course Road",
                        28.4595,
                        77.0266,
                        "HIGH",
                        "TRAUMA",
                        "Road accident",
                        "RESPONDING",
                        LocalDateTime.of(
                                2026,
                                9,
                                13,
                                10,
                                30
                        )
                );


        when(
                emergencyService.updateStatus(
                        101L,
                        "RESPONDING"
                )
        ).thenReturn(response);


        mockMvc.perform(
                        put(
                                "/api/emergencies/101/status"
                        )
                                .param(
                                        "status",
                                        "RESPONDING"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("RESPONDING")
                );


        verify(
                emergencyService
        ).updateStatus(
                101L,
                "RESPONDING"
        );
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // POST /api/emergencies
    //
    // Missing required fields should fail validation.
    //
    // The service must NOT be called because validation happens
    // before the controller method executes.
    //
    // =========================================================

    @Test
    void shouldRejectInvalidEmergencyRequest() throws Exception {

        String invalidRequest = """
                {
                    "location": "",
                    "latitude": null,
                    "longitude": null,
                    "priority": "",
                    "facility": "",
                    "notes": ""
                }
                """;


        mockMvc.perform(
                        post("/api/emergencies")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(invalidRequest)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("VALIDATION_ERROR")
                );
    }


    // =========================================================
    // TEST 6
    // =========================================================
    //
    // Invalid latitude/longitude should be rejected by
    // @DecimalMin / @DecimalMax validation.
    //
    // =========================================================

    @Test
    void shouldRejectInvalidCoordinates() throws Exception {

        String invalidRequest = """
                {
                    "location": "Golf Course Road",
                    "latitude": 100.0,
                    "longitude": 200.0,
                    "priority": "HIGH",
                    "facility": "TRAUMA",
                    "notes": "Road accident"
                }
                """;


        mockMvc.perform(
                        post("/api/emergencies")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(invalidRequest)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("VALIDATION_ERROR")
                );
    }
}