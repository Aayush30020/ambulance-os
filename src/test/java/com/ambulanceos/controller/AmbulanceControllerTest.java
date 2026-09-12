package com.ambulanceos.controller;

import com.ambulanceos.dto.AmbulanceRequest;
import com.ambulanceos.dto.AmbulanceResponse;
import com.ambulanceos.exception.GlobalExceptionHandler;
import com.ambulanceos.service.AmbulanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
class AmbulanceControllerTest {

    @Mock
    private AmbulanceService ambulanceService;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {

        AmbulanceController controller =
                new AmbulanceController(
                        ambulanceService
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
    // POST /api/ambulances
    //
    // Valid request should create an ambulance and return
    // HTTP 201 CREATED.
    //
    // =========================================================

    @Test
    void shouldCreateAmbulance() throws Exception {

        AmbulanceResponse response =
                new AmbulanceResponse(
                        101L,
                        "AMB-101",
                        28.4598,
                        77.0268,
                        "AVAILABLE",
                        "ALS"
                );


        when(
                ambulanceService.createAmbulance(
                        any(AmbulanceRequest.class)
                )
        ).thenReturn(response);


        String requestJson = """
                {
                    "ambulanceNumber": "AMB-101",
                    "latitude": 28.4598,
                    "longitude": 77.0268,
                    "status": "AVAILABLE",
                    "type": "ALS"
                }
                """;


        mockMvc.perform(
                        post("/api/ambulances")
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
                        jsonPath("$.ambulanceNumber")
                                .value("AMB-101")
                )
                .andExpect(
                        jsonPath("$.latitude")
                                .value(28.4598)
                )
                .andExpect(
                        jsonPath("$.longitude")
                                .value(77.0268)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("AVAILABLE")
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("ALS")
                );


        verify(
                ambulanceService
        ).createAmbulance(
                any(AmbulanceRequest.class)
        );
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // GET /api/ambulances
    //
    // Should return all ambulances with HTTP 200.
    //
    // =========================================================

    @Test
    void shouldGetAllAmbulances() throws Exception {

        AmbulanceResponse firstAmbulance =
                new AmbulanceResponse(
                        101L,
                        "AMB-101",
                        28.4598,
                        77.0268,
                        "AVAILABLE",
                        "ALS"
                );


        AmbulanceResponse secondAmbulance =
                new AmbulanceResponse(
                        102L,
                        "AMB-102",
                        28.4675,
                        77.0312,
                        "EN_ROUTE",
                        "ALS"
                );


        when(
                ambulanceService.getAllAmbulances()
        ).thenReturn(
                List.of(
                        firstAmbulance,
                        secondAmbulance
                )
        );


        mockMvc.perform(
                        get("/api/ambulances")
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
                        jsonPath("$[0].ambulanceNumber")
                                .value("AMB-101")
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("AVAILABLE")
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
                                .value("EN_ROUTE")
                );


        verify(
                ambulanceService
        ).getAllAmbulances();
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // GET /api/ambulances/{id}
    //
    // Should return the requested ambulance.
    //
    // =========================================================

    @Test
    void shouldGetAmbulanceById() throws Exception {

        AmbulanceResponse response =
                new AmbulanceResponse(
                        101L,
                        "AMB-101",
                        28.4598,
                        77.0268,
                        "AVAILABLE",
                        "ALS"
                );


        when(
                ambulanceService.getAmbulanceById(101L)
        ).thenReturn(response);


        mockMvc.perform(
                        get("/api/ambulances/101")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.ambulanceNumber")
                                .value("AMB-101")
                )
                .andExpect(
                        jsonPath("$.latitude")
                                .value(28.4598)
                )
                .andExpect(
                        jsonPath("$.longitude")
                                .value(77.0268)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("AVAILABLE")
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("ALS")
                );


        verify(
                ambulanceService
        ).getAmbulanceById(101L);
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // PUT /api/ambulances/{id}/status?status=EN_ROUTE
    //
    // Should update the ambulance status.
    //
    // =========================================================

    @Test
    void shouldUpdateAmbulanceStatus() throws Exception {

        AmbulanceResponse response =
                new AmbulanceResponse(
                        101L,
                        "AMB-101",
                        28.4598,
                        77.0268,
                        "EN_ROUTE",
                        "ALS"
                );


        when(
                ambulanceService.updateStatus(
                        101L,
                        "EN_ROUTE"
                )
        ).thenReturn(response);


        mockMvc.perform(
                        put(
                                "/api/ambulances/101/status"
                        )
                                .param(
                                        "status",
                                        "EN_ROUTE"
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
                        jsonPath("$.ambulanceNumber")
                                .value("AMB-101")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("EN_ROUTE")
                );


        verify(
                ambulanceService
        ).updateStatus(
                101L,
                "EN_ROUTE"
        );
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // POST /api/ambulances
    //
    // Missing required fields should fail validation.
    //
    // The service should NOT be called because @Valid rejects
    // the request before the controller method executes.
    //
    // =========================================================

    @Test
    void shouldRejectInvalidAmbulanceRequest() throws Exception {

        String invalidRequest = """
                {
                    "ambulanceNumber": "",
                    "latitude": null,
                    "longitude": null,
                    "status": "",
                    "type": ""
                }
                """;


        mockMvc.perform(
                        post("/api/ambulances")
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
    // Invalid coordinates should fail validation.
    //
    // =========================================================

    @Test
    void shouldRejectInvalidAmbulanceCoordinates() throws Exception {

        String invalidRequest = """
                {
                    "ambulanceNumber": "AMB-101",
                    "latitude": 100.0,
                    "longitude": 200.0,
                    "status": "AVAILABLE",
                    "type": "ALS"
                }
                """;


        mockMvc.perform(
                        post("/api/ambulances")
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