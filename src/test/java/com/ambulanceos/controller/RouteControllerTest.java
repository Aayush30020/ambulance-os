package com.ambulanceos.controller;

import com.ambulanceos.dto.RouteResponse;
import com.ambulanceos.exception.AmbulanceNotFoundException;
import com.ambulanceos.exception.GlobalExceptionHandler;
import com.ambulanceos.exception.HospitalNotFoundException;
import com.ambulanceos.exception.RouteNotFoundException;
import com.ambulanceos.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RouteControllerTest {

    @Mock
    private RouteService routeService;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {

        RouteController controller =
                new RouteController(
                        routeService
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
    // GET
    // /api/routes/ambulance/{ambulanceId}/hospital/{hospitalId}
    //
    // Successful route request should return the complete
    // route information.
    //
    // =========================================================

    @Test
    void shouldFindRouteSuccessfully() throws Exception {

        List<String> nodePath =
                List.of(
                        "node-100",
                        "node-101",
                        "node-102",
                        "node-103"
                );


        List<RouteResponse.RoutePoint> routePoints =
                List.of(
                        new RouteResponse.RoutePoint(
                                28.4598,
                                77.0268
                        ),
                        new RouteResponse.RoutePoint(
                                28.4589,
                                77.0301
                        ),
                        new RouteResponse.RoutePoint(
                                28.4565,
                                77.0350
                        ),
                        new RouteResponse.RoutePoint(
                                28.4352,
                                77.0817
                        )
                );


        RouteResponse response =
                new RouteResponse(
                        101L,
                        "AMB-101",
                        201L,
                        "Artemis Hospital",
                        "node-100",
                        "node-103",
                        7.85,
                        14.25,
                        "MODERATE",
                        nodePath,
                        routePoints
                );


        when(
                routeService.findRoute(
                        101L,
                        201L
                )
        ).thenReturn(response);


        mockMvc.perform(
                        get(
                                "/api/routes/ambulance/101/hospital/201"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.ambulanceId")
                                .value(101)
                )
                .andExpect(
                        jsonPath("$.ambulanceNumber")
                                .value("AMB-101")
                )
                .andExpect(
                        jsonPath("$.hospitalId")
                                .value(201)
                )
                .andExpect(
                        jsonPath("$.hospitalName")
                                .value("Artemis Hospital")
                )
                .andExpect(
                        jsonPath("$.sourceNode")
                                .value("node-100")
                )
                .andExpect(
                        jsonPath("$.destinationNode")
                                .value("node-103")
                )
                .andExpect(
                        jsonPath("$.distanceKm")
                                .value(7.85)
                )
                .andExpect(
                        jsonPath("$.estimatedTravelTimeMinutes")
                                .value(14.25)
                )
                .andExpect(
                        jsonPath("$.trafficLevel")
                                .value("MODERATE")
                )
                .andExpect(
                        jsonPath("$.nodePath.length()")
                                .value(4)
                )
                .andExpect(
                        jsonPath("$.nodePath[0]")
                                .value("node-100")
                )
                .andExpect(
                        jsonPath("$.nodePath[3]")
                                .value("node-103")
                )
                .andExpect(
                        jsonPath("$.route.length()")
                                .value(4)
                )
                .andExpect(
                        jsonPath("$.route[0].latitude")
                                .value(28.4598)
                )
                .andExpect(
                        jsonPath("$.route[0].longitude")
                                .value(77.0268)
                );


        verify(
                routeService
        ).findRoute(
                101L,
                201L
        );
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // Verify that the controller correctly passes both path
    // variables to RouteService.
    //
    // =========================================================

    @Test
    void shouldPassCorrectIdsToRouteService()
            throws Exception {

        List<String> nodePath =
                List.of(
                        "source-node",
                        "middle-node",
                        "destination-node"
                );


        List<RouteResponse.RoutePoint> routePoints =
                List.of(
                        new RouteResponse.RoutePoint(
                                28.4500,
                                77.0400
                        ),
                        new RouteResponse.RoutePoint(
                                28.4450,
                                77.0500
                        ),
                        new RouteResponse.RoutePoint(
                                28.4246,
                                76.9957
                        )
                );


        RouteResponse response =
                new RouteResponse(
                        104L,
                        "AMB-104",
                        301L,
                        "Medanta Hospital",
                        "source-node",
                        "destination-node",
                        6.40,
                        12.10,
                        "LOW",
                        nodePath,
                        routePoints
                );


        when(
                routeService.findRoute(
                        104L,
                        301L
                )
        ).thenReturn(response);


        mockMvc.perform(
                        get(
                                "/api/routes/ambulance/104/hospital/301"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.ambulanceId")
                                .value(104)
                )
                .andExpect(
                        jsonPath("$.hospitalId")
                                .value(301)
                );


        verify(
                routeService
        ).findRoute(
                104L,
                301L
        );
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // Missing ambulance should return:
    //
    // HTTP 404
    // AMBULANCE_NOT_FOUND
    //
    // =========================================================

    @Test
    void shouldReturn404WhenAmbulanceDoesNotExist()
            throws Exception {

        when(
                routeService.findRoute(
                        999L,
                        201L
                )
        ).thenThrow(
                new AmbulanceNotFoundException(999L)
        );


        mockMvc.perform(
                        get(
                                "/api/routes/ambulance/999/hospital/201"
                        )
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
                                .value("AMBULANCE_NOT_FOUND")
                );


        verify(
                routeService
        ).findRoute(
                999L,
                201L
        );
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // Missing hospital should return:
    //
    // HTTP 404
    // HOSPITAL_NOT_FOUND
    //
    // =========================================================

    @Test
    void shouldReturn404WhenHospitalDoesNotExist()
            throws Exception {

        when(
                routeService.findRoute(
                        101L,
                        999L
                )
        ).thenThrow(
                new HospitalNotFoundException(999L)
        );


        mockMvc.perform(
                        get(
                                "/api/routes/ambulance/101/hospital/999"
                        )
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
                                .value("HOSPITAL_NOT_FOUND")
                );


        verify(
                routeService
        ).findRoute(
                101L,
                999L
        );
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // If the routing system cannot find a valid route, the
    // service throws RouteNotFoundException.
    //
    // GlobalExceptionHandler should return:
    //
    // HTTP 404
    // ROUTE_NOT_FOUND
    //
    // =========================================================

    @Test
    void shouldReturn404WhenRouteDoesNotExist()
            throws Exception {

        when(
                routeService.findRoute(
                        101L,
                        201L
                )
        ).thenThrow(
                new RouteNotFoundException(
                        "No route found between ambulance and hospital"
                )
        );


        mockMvc.perform(
                        get(
                                "/api/routes/ambulance/101/hospital/201"
                        )
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
                                .value("ROUTE_NOT_FOUND")
                );


        verify(
                routeService
        ).findRoute(
                101L,
                201L
        );
    }
}