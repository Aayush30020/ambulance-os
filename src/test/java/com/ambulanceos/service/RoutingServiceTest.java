package com.ambulanceos.service;

import com.ambulanceos.dto.TrafficDijkstraResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoutingServiceTest {

    private TrafficAwareDijkstraService trafficAwareDijkstraService;
    private AStarService aStarService;
    private RoutingSettingsService routingSettingsService;

    private RoutingService routingService;

    private TrafficDijkstraResponse dijkstraResponse;
    private TrafficDijkstraResponse aStarResponse;

    @BeforeEach
    void setUp() {

        trafficAwareDijkstraService =
                mock(TrafficAwareDijkstraService.class);

        aStarService =
                mock(AStarService.class);

        routingSettingsService =
                mock(RoutingSettingsService.class);

        routingService =
                new RoutingService(
                        trafficAwareDijkstraService,
                        aStarService,
                        routingSettingsService
                );

        dijkstraResponse =
                new TrafficDijkstraResponse(
                        "NODE-A",
                        "NODE-B",
                        5.25,
                        8.50,
                        "MODERATE",
                        java.util.List.of(
                                "NODE-A",
                                "NODE-B"
                        )
                );

        aStarResponse =
                new TrafficDijkstraResponse(
                        "NODE-A",
                        "NODE-B",
                        5.25,
                        8.50,
                        "MODERATE",
                        java.util.List.of(
                                "NODE-A",
                                "NODE-B"
                        )
                );
    }


    // =========================================================
    // DIJKSTRA SELECTION
    // =========================================================

    @Test
    void shouldUseDijkstraWhenDijkstraIsSelected() {

        when(
                trafficAwareDijkstraService.findShortestPath(
                        "NODE-A",
                        "NODE-B"
                )
        ).thenReturn(dijkstraResponse);

        TrafficDijkstraResponse result =
                routingService.findRoute(
                        "NODE-A",
                        "NODE-B",
                        RoutingAlgorithm.DIJKSTRA
                );

        assertNotNull(result);

        assertSame(
                dijkstraResponse,
                result
        );

        verify(
                trafficAwareDijkstraService
        ).findShortestPath(
                "NODE-A",
                "NODE-B"
        );

        verifyNoInteractions(aStarService);
    }


    // =========================================================
    // A* SELECTION
    // =========================================================

    @Test
    void shouldUseAStarWhenAStarIsSelected() {

        when(
                aStarService.findShortestPath(
                        "NODE-A",
                        "NODE-B"
                )
        ).thenReturn(aStarResponse);

        TrafficDijkstraResponse result =
                routingService.findRoute(
                        "NODE-A",
                        "NODE-B",
                        RoutingAlgorithm.ASTAR
                );

        assertNotNull(result);

        assertSame(
                aStarResponse,
                result
        );

        verify(
                aStarService
        ).findShortestPath(
                "NODE-A",
                "NODE-B"
        );

        verifyNoInteractions(
                trafficAwareDijkstraService
        );
    }


    // =========================================================
    // NULL ALGORITHM
    // =========================================================

    @Test
    void shouldRejectNullRoutingAlgorithm() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> routingService.findRoute(
                                "NODE-A",
                                "NODE-B",
                                null
                        )
                );

        assertEquals(
                "Routing algorithm cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                trafficAwareDijkstraService,
                aStarService
        );
    }


    // =========================================================
    // CURRENT ALGORITHM
    // =========================================================

    @Test
    void shouldReturnCurrentRoutingAlgorithm() {

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        RoutingAlgorithm result =
                routingService.getCurrentAlgorithm();

        assertEquals(
                RoutingAlgorithm.DIJKSTRA,
                result
        );

        verify(
                routingSettingsService
        ).getRoutingAlgorithm();
    }


    // =========================================================
    // SET CURRENT ALGORITHM
    // =========================================================

    @Test
    void shouldSetCurrentRoutingAlgorithm() {

        when(
                routingSettingsService.setRoutingAlgorithm(
                        RoutingAlgorithm.ASTAR
                )
        ).thenReturn(
                RoutingAlgorithm.ASTAR
        );

        RoutingAlgorithm result =
                routingService.setCurrentAlgorithm(
                        RoutingAlgorithm.ASTAR
                );

        assertEquals(
                RoutingAlgorithm.ASTAR,
                result
        );

        verify(
                routingSettingsService
        ).setRoutingAlgorithm(
                RoutingAlgorithm.ASTAR
        );
    }


    // =========================================================
    // CURRENT SETTING SHOULD CONTROL DEFAULT ROUTING
    // =========================================================

    @Test
    void shouldUseConfiguredAlgorithmWhenNoAlgorithmIsProvided() {

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.ASTAR
        );

        when(
                aStarService.findShortestPath(
                        "NODE-A",
                        "NODE-B"
                )
        ).thenReturn(aStarResponse);

        TrafficDijkstraResponse result =
                routingService.findRoute(
                        "NODE-A",
                        "NODE-B"
                );

        assertNotNull(result);

        assertSame(
                aStarResponse,
                result
        );

        verify(
                routingSettingsService
        ).getRoutingAlgorithm();

        verify(
                aStarService
        ).findShortestPath(
                "NODE-A",
                "NODE-B"
        );

        verifyNoInteractions(
                trafficAwareDijkstraService
        );
    }
}