package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.exception.NoAvailableAmbulanceException;
import com.ambulanceos.exception.RouteNotFoundException;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.repository.EmergencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private DispatchRepository dispatchRepository;

    @Mock
    private GurgaonRoadGraph roadGraph;

    @Mock
    private RoutingService routingService;

    @Mock
    private RoutingSettingsService routingSettingsService;

    @InjectMocks
    private DispatchService dispatchService;


    private Emergency emergency;

    private GraphNode emergencyNode;


    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() {

        emergency =
                Emergency.builder()
                        .id(1L)
                        .location("Sector 29, Gurgaon")
                        .latitude(28.4595)
                        .longitude(77.0266)
                        .priority("HIGH")
                        .facility("TRAUMA")
                        .status("ACTIVE")
                        .build();

        emergencyNode =
                new GraphNode(
                        "emergency-node",
                        "Emergency",
                        28.4595,
                        77.0266
                );
    }


    // =========================================================
    // TEST 1
    // FASTEST AVAILABLE AMBULANCE SHOULD BE SELECTED
    // =========================================================

    @Test
    void shouldSelectFastestAvailableAmbulance() {

        /*
         * DispatchService reads the routing configuration when
         * calculating the best ambulance.
         */
        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );


        Ambulance ambulance1 =
                Ambulance.builder()
                        .id(101L)
                        .ambulanceNumber("AMB-101")
                        .latitude(28.4500)
                        .longitude(77.0200)
                        .status("AVAILABLE")
                        .type("ALS")
                        .build();

        Ambulance ambulance2 =
                Ambulance.builder()
                        .id(102L)
                        .ambulanceNumber("AMB-102")
                        .latitude(28.4600)
                        .longitude(77.0300)
                        .status("AVAILABLE")
                        .type("ALS")
                        .build();


        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );


        when(
                roadGraph.findNearestNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                )
        ).thenReturn(
                emergencyNode
        );


        when(
                ambulanceRepository.findByStatusIgnoreCase(
                        "AVAILABLE"
                )
        ).thenReturn(
                List.of(
                        ambulance1,
                        ambulance2
                )
        );


        GraphNode ambulanceNode1 =
                new GraphNode(
                        "ambulance-node-1",
                        "AMB-101",
                        28.4500,
                        77.0200
                );

        GraphNode ambulanceNode2 =
                new GraphNode(
                        "ambulance-node-2",
                        "AMB-102",
                        28.4600,
                        77.0300
                );


        when(
                roadGraph.findNearestNode(
                        ambulance1.getLatitude(),
                        ambulance1.getLongitude()
                )
        ).thenReturn(
                ambulanceNode1
        );


        when(
                roadGraph.findNearestNode(
                        ambulance2.getLatitude(),
                        ambulance2.getLongitude()
                )
        ).thenReturn(
                ambulanceNode2
        );


        TrafficDijkstraResponse route1 =
                new TrafficDijkstraResponse(
                        "ambulance-node-1",
                        "emergency-node",
                        5.0,
                        8.0,
                        "MODERATE",
                        List.of(
                                "ambulance-node-1",
                                "emergency-node"
                        )
                );


        TrafficDijkstraResponse route2 =
                new TrafficDijkstraResponse(
                        "ambulance-node-2",
                        "emergency-node",
                        3.0,
                        5.0,
                        "LOW",
                        List.of(
                                "ambulance-node-2",
                                "emergency-node"
                        )
                );


        /*
         * Current RoutingService API requires the routing
         * algorithm explicitly.
         */
        when(
                routingService.findRoute(
                        "ambulance-node-1",
                        "emergency-node",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenReturn(
                route1
        );


        when(
                routingService.findRoute(
                        "ambulance-node-2",
                        "emergency-node",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenReturn(
                route2
        );


        DispatchResponse result =
                dispatchService.findBestAmbulance(1L);


        assertNotNull(result);

        assertEquals(
                102L,
                result.ambulanceId()
        );

        assertEquals(
                "AMB-102",
                result.ambulanceNumber()
        );

        assertEquals(
                3.0,
                result.distanceKm()
        );

        assertEquals(
                5.0,
                result.estimatedTravelTimeMinutes()
        );

        assertEquals(
                "LOW",
                result.trafficLevel()
        );
    }


    // =========================================================
    // TEST 2
    // NO AVAILABLE AMBULANCE
    // =========================================================

    @Test
    void shouldThrowExceptionWhenNoAmbulanceIsAvailable() {

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );


        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );


        when(
                roadGraph.findNearestNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                )
        ).thenReturn(
                emergencyNode
        );


        when(
                ambulanceRepository.findByStatusIgnoreCase(
                        "AVAILABLE"
                )
        ).thenReturn(
                List.of()
        );


        assertThrows(
                NoAvailableAmbulanceException.class,
                () ->
                        dispatchService.findBestAmbulance(1L)
        );
    }


    // =========================================================
    // TEST 3
    // UNREACHABLE AMBULANCE SHOULD BE SKIPPED
    // =========================================================

    @Test
    void shouldSkipUnreachableAmbulance() {

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );


        Ambulance unreachableAmbulance =
                Ambulance.builder()
                        .id(101L)
                        .ambulanceNumber("AMB-101")
                        .latitude(28.4500)
                        .longitude(77.0200)
                        .status("AVAILABLE")
                        .type("ALS")
                        .build();


        Ambulance reachableAmbulance =
                Ambulance.builder()
                        .id(102L)
                        .ambulanceNumber("AMB-102")
                        .latitude(28.4600)
                        .longitude(77.0300)
                        .status("AVAILABLE")
                        .type("BLS")
                        .build();


        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );


        when(
                roadGraph.findNearestNode(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                )
        ).thenReturn(
                emergencyNode
        );


        when(
                ambulanceRepository.findByStatusIgnoreCase(
                        "AVAILABLE"
                )
        ).thenReturn(
                List.of(
                        unreachableAmbulance,
                        reachableAmbulance
                )
        );


        GraphNode unreachableNode =
                new GraphNode(
                        "unreachable-node",
                        "AMB-101",
                        28.4500,
                        77.0200
                );


        GraphNode reachableNode =
                new GraphNode(
                        "reachable-node",
                        "AMB-102",
                        28.4600,
                        77.0300
                );


        when(
                roadGraph.findNearestNode(
                        unreachableAmbulance.getLatitude(),
                        unreachableAmbulance.getLongitude()
                )
        ).thenReturn(
                unreachableNode
        );


        when(
                roadGraph.findNearestNode(
                        reachableAmbulance.getLatitude(),
                        reachableAmbulance.getLongitude()
                )
        ).thenReturn(
                reachableNode
        );


        /*
         * A typed RouteNotFoundException is used because
         * DispatchService intentionally skips unreachable
         * ambulances by catching this exception.
         */
        when(
                routingService.findRoute(
                        "unreachable-node",
                        "emergency-node",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenThrow(
                new RouteNotFoundException(
                        "No route found"
                )
        );


        TrafficDijkstraResponse reachableRoute =
                new TrafficDijkstraResponse(
                        "reachable-node",
                        "emergency-node",
                        4.0,
                        7.0,
                        "MODERATE",
                        List.of(
                                "reachable-node",
                                "emergency-node"
                        )
                );


        when(
                routingService.findRoute(
                        "reachable-node",
                        "emergency-node",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenReturn(
                reachableRoute
        );


        DispatchResponse result =
                dispatchService.findBestAmbulance(1L);


        assertNotNull(result);

        assertEquals(
                102L,
                result.ambulanceId()
        );

        assertEquals(
                "AMB-102",
                result.ambulanceNumber()
        );
    }


    // =========================================================
    // TEST 4
    // COMPLETING DISPATCH SHOULD RELEASE AMBULANCE
    // =========================================================

    @Test
    void shouldReturnAmbulanceToAvailableWhenDispatchIsCompleted() {

        Ambulance ambulance =
                Ambulance.builder()
                        .id(101L)
                        .ambulanceNumber("AMB-101")
                        .latitude(28.4500)
                        .longitude(77.0200)
                        .status("EN_ROUTE")
                        .type("ALS")
                        .build();


        Dispatch dispatch =
                Dispatch.builder()
                        .id(500L)
                        .emergencyId(1L)
                        .ambulanceId(101L)
                        .ambulanceNumber("AMB-101")
                        .hospitalId(201L)
                        .hospitalName("Medanta Hospital")
                        .routingAlgorithm("DIJKSTRA")
                        .distanceToEmergencyKm(4.0)
                        .timeToEmergencyMinutes(7.0)
                        .distanceToHospitalKm(8.0)
                        .timeToHospitalMinutes(12.0)
                        .totalDistanceKm(12.0)
                        .totalEstimatedTimeMinutes(19.0)
                        .status("IN_PROGRESS")
                        .build();


        when(
                dispatchRepository.findById(500L)
        ).thenReturn(
                Optional.of(dispatch)
        );


        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        when(
                ambulanceRepository.save(ambulance)
        ).thenReturn(
                ambulance
        );


        when(
                dispatchRepository.save(dispatch)
        ).thenReturn(
                dispatch
        );


        Dispatch result =
                dispatchService.completeDispatch(500L);


        assertNotNull(result);


        assertEquals(
                "COMPLETED",
                result.getStatus()
        );


        assertNotNull(
                result.getCompletedAt()
        );


        assertEquals(
                "AVAILABLE",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository
        ).save(
                ambulance
        );


        verify(
                dispatchRepository
        ).save(
                dispatch
        );
    }
}