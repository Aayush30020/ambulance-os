package com.ambulanceos.controller;

import com.ambulanceos.dto.HospitalRequest;
import com.ambulanceos.dto.HospitalResponse;
import com.ambulanceos.exception.GlobalExceptionHandler;
import com.ambulanceos.exception.HospitalNotFoundException;
import com.ambulanceos.service.HospitalService;
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
class HospitalControllerTest {

    @Mock
    private HospitalService hospitalService;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {

        HospitalController controller =
                new HospitalController(
                        hospitalService
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
    // POST /api/hospitals
    //
    // Valid request should create a hospital and return
    // HTTP 201 CREATED.
    //
    // =========================================================

    @Test
    void shouldCreateHospital() throws Exception {

        HospitalResponse response =
                new HospitalResponse(
                        101L,
                        "HOS-01",
                        "Medanta Hospital",
                        28.4246,
                        76.9957,
                        15,
                        "TRAUMA"
                );


        when(
                hospitalService.createHospital(
                        any(HospitalRequest.class)
                )
        ).thenReturn(response);


        String requestJson = """
                {
                    "hospitalCode": "HOS-01",
                    "name": "Medanta Hospital",
                    "latitude": 28.4246,
                    "longitude": 76.9957,
                    "availableBeds": 15,
                    "facilityType": "TRAUMA"
                }
                """;


        mockMvc.perform(
                        post("/api/hospitals")
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
                        jsonPath("$.hospitalCode")
                                .value("HOS-01")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Medanta Hospital")
                )
                .andExpect(
                        jsonPath("$.latitude")
                                .value(28.4246)
                )
                .andExpect(
                        jsonPath("$.longitude")
                                .value(76.9957)
                )
                .andExpect(
                        jsonPath("$.availableBeds")
                                .value(15)
                )
                .andExpect(
                        jsonPath("$.facilityType")
                                .value("TRAUMA")
                );


        verify(
                hospitalService
        ).createHospital(
                any(HospitalRequest.class)
        );
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // GET /api/hospitals
    //
    // Should return all hospitals with HTTP 200.
    //
    // =========================================================

    @Test
    void shouldGetAllHospitals() throws Exception {

        HospitalResponse firstHospital =
                new HospitalResponse(
                        101L,
                        "HOS-01",
                        "Medanta Hospital",
                        28.4246,
                        76.9957,
                        15,
                        "TRAUMA"
                );


        HospitalResponse secondHospital =
                new HospitalResponse(
                        102L,
                        "HOS-02",
                        "Artemis Hospital",
                        28.4352,
                        77.0817,
                        11,
                        "ICU"
                );


        when(
                hospitalService.getAllHospitals()
        ).thenReturn(
                List.of(
                        firstHospital,
                        secondHospital
                )
        );


        mockMvc.perform(
                        get("/api/hospitals")
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
                        jsonPath("$[0].hospitalCode")
                                .value("HOS-01")
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value("Medanta Hospital")
                )
                .andExpect(
                        jsonPath("$[0].availableBeds")
                                .value(15)
                )
                .andExpect(
                        jsonPath("$[0].facilityType")
                                .value("TRAUMA")
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(102)
                )
                .andExpect(
                        jsonPath("$[1].hospitalCode")
                                .value("HOS-02")
                )
                .andExpect(
                        jsonPath("$[1].name")
                                .value("Artemis Hospital")
                )
                .andExpect(
                        jsonPath("$[1].availableBeds")
                                .value(11)
                )
                .andExpect(
                        jsonPath("$[1].facilityType")
                                .value("ICU")
                );


        verify(
                hospitalService
        ).getAllHospitals();
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // GET /api/hospitals/{id}
    //
    // Should return the requested hospital.
    //
    // =========================================================

    @Test
    void shouldGetHospitalById() throws Exception {

        HospitalResponse response =
                new HospitalResponse(
                        101L,
                        "HOS-01",
                        "Medanta Hospital",
                        28.4246,
                        76.9957,
                        15,
                        "TRAUMA"
                );


        when(
                hospitalService.getHospitalById(101L)
        ).thenReturn(response);


        mockMvc.perform(
                        get("/api/hospitals/101")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.hospitalCode")
                                .value("HOS-01")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Medanta Hospital")
                )
                .andExpect(
                        jsonPath("$.latitude")
                                .value(28.4246)
                )
                .andExpect(
                        jsonPath("$.longitude")
                                .value(76.9957)
                )
                .andExpect(
                        jsonPath("$.availableBeds")
                                .value(15)
                )
                .andExpect(
                        jsonPath("$.facilityType")
                                .value("TRAUMA")
                );


        verify(
                hospitalService
        ).getHospitalById(101L);
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // PUT /api/hospitals/{id}/beds?availableBeds=20
    //
    // Should update the available bed count.
    //
    // =========================================================

    @Test
    void shouldUpdateAvailableBeds() throws Exception {

        HospitalResponse response =
                new HospitalResponse(
                        101L,
                        "HOS-01",
                        "Medanta Hospital",
                        28.4246,
                        76.9957,
                        20,
                        "TRAUMA"
                );


        when(
                hospitalService.updateBeds(
                        101L,
                        20
                )
        ).thenReturn(response);


        mockMvc.perform(
                        put("/api/hospitals/101/beds")
                                .param(
                                        "availableBeds",
                                        "20"
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
                        jsonPath("$.hospitalCode")
                                .value("HOS-01")
                )
                .andExpect(
                        jsonPath("$.availableBeds")
                                .value(20)
                );


        verify(
                hospitalService
        ).updateBeds(
                101L,
                20
        );
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // POST /api/hospitals
    //
    // Missing required fields should fail validation.
    //
    // @Valid rejects the request before the service method
    // is called.
    //
    // =========================================================

    @Test
    void shouldRejectInvalidHospitalRequest() throws Exception {

        String invalidRequest = """
                {
                    "hospitalCode": "",
                    "name": "",
                    "latitude": null,
                    "longitude": null,
                    "availableBeds": null,
                    "facilityType": ""
                }
                """;


        mockMvc.perform(
                        post("/api/hospitals")
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
    void shouldRejectInvalidHospitalCoordinates() throws Exception {

        String invalidRequest = """
                {
                    "hospitalCode": "HOS-01",
                    "name": "Medanta Hospital",
                    "latitude": 100.0,
                    "longitude": 200.0,
                    "availableBeds": 15,
                    "facilityType": "TRAUMA"
                }
                """;


        mockMvc.perform(
                        post("/api/hospitals")
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
    // TEST 7
    // =========================================================
    //
    // GET /api/hospitals/{id}
    //
    // Missing hospital should return:
    //
    // HTTP 404
    // HOSPITAL_NOT_FOUND
    //
    // =========================================================

    @Test
    void shouldReturn404WhenHospitalDoesNotExist() throws Exception {

        when(
                hospitalService.getHospitalById(999L)
        ).thenThrow(
                new HospitalNotFoundException(999L)
        );


        mockMvc.perform(
                        get("/api/hospitals/999")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("HOSPITAL_NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }


    // =========================================================
    // TEST 8
    // =========================================================
    //
    // PUT /api/hospitals/{id}/beds?availableBeds=-5
    //
    // The service rejects negative bed counts with
    // IllegalArgumentException.
    //
    // GlobalExceptionHandler should convert this to HTTP 400.
    //
    // =========================================================

    @Test
    void shouldRejectNegativeAvailableBeds() throws Exception {

        when(
                hospitalService.updateBeds(
                        101L,
                        -5
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "Available beds cannot be negative"
                )
        );


        mockMvc.perform(
                        put("/api/hospitals/101/beds")
                                .param(
                                        "availableBeds",
                                        "-5"
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("INVALID_REQUEST")
                );
    }
}