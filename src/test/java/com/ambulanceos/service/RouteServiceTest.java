package com.ambulanceos.service;

import com.ambulanceos.dto.RouteResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.exception.AmbulanceNotFoundException;
import com.ambulanceos.exception.HospitalNotFoundException;
import com.ambulanceos.exception.RouteNotFoundException;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.HospitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RouteServiceTest {

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private GurgaonRoadGraph roadGraph;

    @Mock
    private RoutingService routingService;

    @InjectMocks
    private RouteService routeService;

    private Ambulance ambulance;
    private Hospital hospital;

    private GraphNode sourceNode;
    private GraphNode destinationNode;


    @BeforeEach
    void setUp() {

        ambulance =
                org.mockito.Mockito.mock(Ambulance.class);

        hospital =
                org.mockito.Mockito.mock(Hospital.class);

        sourceNode =
                new GraphNode(
                        "ambulance-node",
                        "Ambulance Node",
                        28.4595,
                        77.0266
                );

        destinationNode =
                new GraphNode(
                        "hospital-node",
                        "Hospital Node",
                        28.4500,
                        77.0400
                );


        when(ambulance.getId())
                .thenReturn(101L);

        when(ambulance.getAmbulanceNumber())
                .thenReturn("AMB-101");

        when(ambulance.getLatitude())
                .thenReturn(28.4595);

        when(ambulance.getLongitude())
                .thenReturn(77.0266);


        when(hospital.getId())
                .thenReturn(201L);

        when(hospital.getName())
                .thenReturn("Medanta Hospital");

        when(hospital.getLatitude())
                .thenReturn(28.4500);

        when(hospital.getLongitude())
                .thenReturn(77.0400);


        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );

        when(
                hospitalRepository.findById(201L)
        ).thenReturn(
                Optional.of(hospital)
        );


        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(sourceNode);

        when(
                roadGraph.findNearestNode(
                        28.4500,
                        77.0400
                )
        ).thenReturn(destinationNode);
    }


    // =========================================================
    // TEST 1
    // =========================================================
    //
    // Normal route calculation should return a complete
    // RouteResponse.
    //
    // =========================================================

    @Test
    void shouldCalculateRouteSuccessfully() {

        List<String> nodePath =
                List.of(
                        "ambulance-node",
                        "middle-node",
                        "hospital-node"
                );


        TrafficDijkstraResponse trafficRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );


        when(
                trafficRoute.path()
        ).thenReturn(nodePath);

        when(
                trafficRoute.distanceKm()
        ).thenReturn(7.5);

        when(
                trafficRoute.estimatedTravelTimeMinutes()
        ).thenReturn(12.0);

        when(
                trafficRoute.trafficLevel()
        ).thenReturn("MODERATE");


        GraphNode middleNode =
                new GraphNode(
                        "middle-node",
                        "Middle Node",
                        28.4550,
                        77.0330
                );


        when(
                roadGraph.getNode("ambulance-node")
        ).thenReturn(sourceNode);

        when(
                roadGraph.getNode("middle-node")
        ).thenReturn(middleNode);

        when(
                roadGraph.getNode("hospital-node")
        ).thenReturn(destinationNode);


        when(
                routingService.getCurrentAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );


        when(
                routingService.findRoute(
                        "ambulance-node",
                        "hospital-node"
                )
        ).thenReturn(trafficRoute);


        RouteResponse result =
                routeService.findRoute(
                        101L,
                        201L
                );


        assertNotNull(result);


        assertEquals(
                101L,
                result.ambulanceId()
        );

        assertEquals(
                "AMB-101",
                result.ambulanceNumber()
        );

        assertEquals(
                201L,
                result.hospitalId()
        );

        assertEquals(
                "Medanta Hospital",
                result.hospitalName()
        );

        assertEquals(
                "ambulance-node",
                result.sourceNode()
        );

        assertEquals(
                "hospital-node",
                result.destinationNode()
        );

        assertEquals(
                7.5,
                result.distanceKm()
        );

        assertEquals(
                12.0,
                result.estimatedTravelTimeMinutes()
        );

        assertEquals(
                "MODERATE",
                result.trafficLevel()
        );

        assertEquals(
                nodePath,
                result.nodePath()
        );

        assertNotNull(
                result.route()
        );

        assertEquals(
                3,
                result.route().size()
        );
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // Missing ambulance should produce the typed exception.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenAmbulanceDoesNotExist() {

        when(
                ambulanceRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );


        assertThrows(
                AmbulanceNotFoundException.class,
                () ->
                        routeService.findRoute(
                                999L,
                                201L
                        )
        );


        verify(
                hospitalRepository,
                never()
        ).findById(201L);

        verify(
                roadGraph,
                never()
        ).findNearestNode(
                org.mockito.Mockito.anyDouble(),
                org.mockito.Mockito.anyDouble()
        );
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // Missing hospital should produce the typed exception.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenHospitalDoesNotExist() {

        when(
                hospitalRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );


        assertThrows(
                HospitalNotFoundException.class,
                () ->
                        routeService.findRoute(
                                101L,
                                999L
                        )
        );


        verify(
                roadGraph,
                never()
        ).findNearestNode(
                org.mockito.Mockito.anyDouble(),
                org.mockito.Mockito.anyDouble()
        );
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // If the ambulance cannot be mapped to the road graph,
    // routing cannot proceed.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenAmbulanceRoadNodeIsMissing() {

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(null);


        assertThrows(
                RouteNotFoundException.class,
                () ->
                        routeService.findRoute(
                                101L,
                                201L
                        )
        );


        verify(
                routingService,
                never()
        ).findRoute(
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.anyString()
        );
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // If the hospital cannot be mapped to the road graph,
    // routing cannot proceed.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenHospitalRoadNodeIsMissing() {

        when(
                roadGraph.findNearestNode(
                        28.4500,
                        77.0400
                )
        ).thenReturn(null);


        assertThrows(
                RouteNotFoundException.class,
                () ->
                        routeService.findRoute(
                                101L,
                                201L
                        )
        );


        verify(
                routingService,
                never()
        ).findRoute(
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.anyString()
        );
    }


    // =========================================================
    // TEST 6
    // =========================================================
    //
    // RoutingService may fail because no graph path exists.
    //
    // RouteService should translate that RuntimeException
    // into RouteNotFoundException.
    //
    // =========================================================

    @Test
    void shouldThrowRouteNotFoundWhenRoutingFails() {

        when(
                routingService.findRoute(
                        "ambulance-node",
                        "hospital-node"
                )
        ).thenThrow(
                new RuntimeException(
                        "No path found"
                )
        );


        assertThrows(
                RouteNotFoundException.class,
                () ->
                        routeService.findRoute(
                                101L,
                                201L
                        )
        );
    }


    // =========================================================
    // TEST 7
    // =========================================================
    //
    // Routing algorithm returning an empty path is invalid.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenRoutePathIsEmpty() {

        TrafficDijkstraResponse trafficRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );


        when(
                trafficRoute.path()
        ).thenReturn(
                List.of()
        );


        when(
                routingService.findRoute(
                        "ambulance-node",
                        "hospital-node"
                )
        ).thenReturn(trafficRoute);


        assertThrows(
                RouteNotFoundException.class,
                () ->
                        routeService.findRoute(
                                101L,
                                201L
                        )
        );


        verify(
                routingService
        ).findRoute(
                "ambulance-node",
                "hospital-node"
        );
    }


    // =========================================================
    // TEST 8
    // =========================================================
    //
    // A path containing a graph node that does not exist should
    // be rejected.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenRouteContainsUnknownNode() {

        List<String> nodePath =
                List.of(
                        "ambulance-node",
                        "unknown-node",
                        "hospital-node"
                );


        TrafficDijkstraResponse trafficRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );


        when(
                trafficRoute.path()
        ).thenReturn(nodePath);


        when(
                routingService.findRoute(
                        "ambulance-node",
                        "hospital-node"
                )
        ).thenReturn(trafficRoute);


        when(
                roadGraph.getNode("ambulance-node")
        ).thenReturn(sourceNode);

        when(
                roadGraph.getNode("unknown-node")
        ).thenReturn(null);


        assertThrows(
                RouteNotFoundException.class,
                () ->
                        routeService.findRoute(
                                101L,
                                201L
                        )
        );
    }
}