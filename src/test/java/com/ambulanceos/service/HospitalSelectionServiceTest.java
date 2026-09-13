package com.ambulanceos.service;

import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.exception.EmergencyNotFoundException;
import com.ambulanceos.exception.NoSuitableHospitalException;
import com.ambulanceos.exception.RouteNotFoundException;
import com.ambulanceos.graph.GraphNode;
import com.ambulanceos.graph.GurgaonRoadGraph;
import com.ambulanceos.repository.EmergencyRepository;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HospitalSelectionServiceTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private GurgaonRoadGraph roadGraph;

    @Mock
    private RoutingService routingService;

    @Mock
    private RoutingSettingsService routingSettingsService;

    @InjectMocks
    private HospitalSelectionService hospitalSelectionService;

    private Emergency emergency;

    private GraphNode emergencyNode;

    private GraphNode hospitalNode1;

    private GraphNode hospitalNode2;


    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() {

        emergency =
                org.mockito.Mockito.mock(
                        Emergency.class
                );

        emergencyNode =
                new GraphNode(
                        "emergency-node",
                        "Emergency Node",
                        28.4595,
                        77.0266
                );

        hospitalNode1 =
                new GraphNode(
                        "hospital-node-1",
                        "Hospital Node 1",
                        28.4600,
                        77.0300
                );

        hospitalNode2 =
                new GraphNode(
                        "hospital-node-2",
                        "Hospital Node 2",
                        28.4500,
                        77.0400
                );
    }


    // =========================================================
    // TEST 1
    // FASTEST SUITABLE HOSPITAL SHOULD BE SELECTED
    // =========================================================

    @Test
    void shouldSelectFastestSuitableHospital() {

        when(
                emergency.getId()
        ).thenReturn(
                1L
        );

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergency.getFacility()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                emergencyNode
        );

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        Hospital hospital1 =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        Hospital hospital2 =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        when(
                hospital1.getHospitalCode()
        ).thenReturn(
                "HOS-01"
        );

        when(
                hospital1.getName()
        ).thenReturn(
                "Hospital One"
        );

        when(
                hospital1.getFacilityType()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                hospital1.getAvailableBeds()
        ).thenReturn(
                10
        );

        when(
                hospital1.getLatitude()
        ).thenReturn(
                28.4600
        );

        when(
                hospital1.getLongitude()
        ).thenReturn(
                77.0300
        );

        when(
                hospital2.getHospitalCode()
        ).thenReturn(
                "HOS-02"
        );

        when(
                hospital2.getName()
        ).thenReturn(
                "Hospital Two"
        );

        when(
                hospital2.getFacilityType()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                hospital2.getAvailableBeds()
        ).thenReturn(
                15
        );

        when(
                hospital2.getLatitude()
        ).thenReturn(
                28.4500
        );

        when(
                hospital2.getLongitude()
        ).thenReturn(
                77.0400
        );

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(
                        hospital1,
                        hospital2
                )
        );

        when(
                roadGraph.findNearestNode(
                        28.4600,
                        77.0300
                )
        ).thenReturn(
                hospitalNode1
        );

        when(
                roadGraph.findNearestNode(
                        28.4500,
                        77.0400
                )
        ).thenReturn(
                hospitalNode2
        );

        TrafficDijkstraResponse fastRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(
                fastRoute.distanceKm()
        ).thenReturn(
                5.0
        );

        when(
                fastRoute.estimatedTravelTimeMinutes()
        ).thenReturn(
                8.0
        );

        when(
                fastRoute.trafficLevel()
        ).thenReturn(
                "LOW"
        );

        TrafficDijkstraResponse slowRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(
                slowRoute.distanceKm()
        ).thenReturn(
                8.0
        );

        when(
                slowRoute.estimatedTravelTimeMinutes()
        ).thenReturn(
                15.0
        );

        when(
                slowRoute.trafficLevel()
        ).thenReturn(
                "MODERATE"
        );

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-1",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenReturn(
                fastRoute
        );

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-2",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenReturn(
                slowRoute
        );

        HospitalSelectionResponse result =
                hospitalSelectionService.findBestHospital(
                        1L
                );

        assertNotNull(result);

        assertEquals(
                "HOS-01",
                result.hospitalCode()
        );

        assertEquals(
                "Hospital One",
                result.hospitalName()
        );

        assertEquals(
                10,
                result.availableBeds()
        );

        assertEquals(
                5.0,
                result.distanceKm()
        );

        assertEquals(
                8.0,
                result.estimatedTravelTimeMinutes()
        );

        assertEquals(
                "LOW",
                result.trafficLevel()
        );
    }


    // =========================================================
    // TEST 2
    // HOSPITAL WITH ZERO BEDS MUST BE IGNORED
    // =========================================================

    @Test
    void shouldIgnoreHospitalWithNoAvailableBeds() {

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                emergencyNode
        );

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        Hospital unavailableHospital =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        when(
                unavailableHospital.getAvailableBeds()
        ).thenReturn(
                0
        );

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(
                        unavailableHospital
                )
        );

        assertThrows(
                NoSuitableHospitalException.class,
                () ->
                        hospitalSelectionService
                                .findBestHospital(1L)
        );

        verify(
                routingService,
                never()
        ).findRoute(
                anyString(),
                anyString(),
                any(RoutingAlgorithm.class)
        );
    }


    // =========================================================
    // TEST 3
    // WRONG FACILITY TYPE MUST BE IGNORED
    // =========================================================

    @Test
    void shouldIgnoreHospitalWithWrongFacilityType() {

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergency.getFacility()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                emergencyNode
        );

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        Hospital cardiacHospital =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        when(
                cardiacHospital.getAvailableBeds()
        ).thenReturn(
                20
        );

        when(
                cardiacHospital.getFacilityType()
        ).thenReturn(
                "CARDIAC"
        );

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(
                        cardiacHospital
                )
        );

        assertThrows(
                NoSuitableHospitalException.class,
                () ->
                        hospitalSelectionService
                                .findBestHospital(1L)
        );

        verify(
                routingService,
                never()
        ).findRoute(
                anyString(),
                anyString(),
                any(RoutingAlgorithm.class)
        );
    }


    // =========================================================
    // TEST 4
    // UNREACHABLE HOSPITAL SHOULD BE SKIPPED
    // =========================================================

    @Test
    void shouldSkipUnreachableHospital() {

        when(
                emergency.getId()
        ).thenReturn(
                1L
        );

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergency.getFacility()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                emergencyNode
        );

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        Hospital unreachableHospital =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        Hospital reachableHospital =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        when(
                unreachableHospital.getHospitalCode()
        ).thenReturn(
                "HOS-01"
        );

        when(
                unreachableHospital.getFacilityType()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                unreachableHospital.getAvailableBeds()
        ).thenReturn(
                10
        );

        when(
                unreachableHospital.getLatitude()
        ).thenReturn(
                28.4600
        );

        when(
                unreachableHospital.getLongitude()
        ).thenReturn(
                77.0300
        );

        when(
                reachableHospital.getHospitalCode()
        ).thenReturn(
                "HOS-02"
        );

        when(
                reachableHospital.getName()
        ).thenReturn(
                "Reachable Hospital"
        );

        when(
                reachableHospital.getFacilityType()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                reachableHospital.getAvailableBeds()
        ).thenReturn(
                12
        );

        when(
                reachableHospital.getLatitude()
        ).thenReturn(
                28.4500
        );

        when(
                reachableHospital.getLongitude()
        ).thenReturn(
                77.0400
        );

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(
                        unreachableHospital,
                        reachableHospital
                )
        );

        when(
                roadGraph.findNearestNode(
                        28.4600,
                        77.0300
                )
        ).thenReturn(
                hospitalNode1
        );

        when(
                roadGraph.findNearestNode(
                        28.4500,
                        77.0400
                )
        ).thenReturn(
                hospitalNode2
        );

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-1",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenThrow(
                new RouteNotFoundException(
                        "No route available"
                )
        );

        TrafficDijkstraResponse reachableRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(
                reachableRoute.distanceKm()
        ).thenReturn(
                6.5
        );

        when(
                reachableRoute.estimatedTravelTimeMinutes()
        ).thenReturn(
                11.0
        );

        when(
                reachableRoute.trafficLevel()
        ).thenReturn(
                "MODERATE"
        );

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-2",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenReturn(
                reachableRoute
        );

        HospitalSelectionResponse result =
                hospitalSelectionService.findBestHospital(
                        1L
                );

        assertNotNull(result);

        assertEquals(
                "HOS-02",
                result.hospitalCode()
        );

        assertEquals(
                "Reachable Hospital",
                result.hospitalName()
        );
    }


    // =========================================================
    // TEST 5
    // MORE BEDS SHOULD WIN WHEN TRAVEL TIME IS EQUAL
    // =========================================================

    @Test
    void shouldPreferHospitalWithMoreBedsWhenTravelTimeIsEqual() {

        when(
                emergency.getId()
        ).thenReturn(
                1L
        );

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergency.getFacility()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                emergencyNode
        );

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        Hospital hospitalWithFewerBeds =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        Hospital hospitalWithMoreBeds =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        when(
                hospitalWithFewerBeds.getHospitalCode()
        ).thenReturn(
                "HOS-01"
        );

        when(
                hospitalWithFewerBeds.getFacilityType()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                hospitalWithFewerBeds.getAvailableBeds()
        ).thenReturn(
                5
        );

        when(
                hospitalWithFewerBeds.getLatitude()
        ).thenReturn(
                28.4600
        );

        when(
                hospitalWithFewerBeds.getLongitude()
        ).thenReturn(
                77.0300
        );

        when(
                hospitalWithMoreBeds.getHospitalCode()
        ).thenReturn(
                "HOS-02"
        );

        when(
                hospitalWithMoreBeds.getFacilityType()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                hospitalWithMoreBeds.getAvailableBeds()
        ).thenReturn(
                15
        );

        when(
                hospitalWithMoreBeds.getLatitude()
        ).thenReturn(
                28.4500
        );

        when(
                hospitalWithMoreBeds.getLongitude()
        ).thenReturn(
                77.0400
        );

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(
                        hospitalWithFewerBeds,
                        hospitalWithMoreBeds
                )
        );

        when(
                roadGraph.findNearestNode(
                        28.4600,
                        77.0300
                )
        ).thenReturn(
                hospitalNode1
        );

        when(
                roadGraph.findNearestNode(
                        28.4500,
                        77.0400
                )
        ).thenReturn(
                hospitalNode2
        );

        TrafficDijkstraResponse route1 =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(
                route1.distanceKm()
        ).thenReturn(
                5.0
        );

        when(
                route1.estimatedTravelTimeMinutes()
        ).thenReturn(
                10.0
        );

        when(
                route1.trafficLevel()
        ).thenReturn(
                "LOW"
        );

        TrafficDijkstraResponse route2 =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(
                route2.distanceKm()
        ).thenReturn(
                7.0
        );

        when(
                route2.estimatedTravelTimeMinutes()
        ).thenReturn(
                10.0
        );

        when(
                route2.trafficLevel()
        ).thenReturn(
                "LOW"
        );

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-1",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenReturn(
                route1
        );

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-2",
                        RoutingAlgorithm.DIJKSTRA
                )
        ).thenReturn(
                route2
        );

        HospitalSelectionResponse result =
                hospitalSelectionService.findBestHospital(
                        1L
                );

        assertNotNull(result);

        assertEquals(
                "HOS-02",
                result.hospitalCode()
        );

        assertEquals(
                15,
                result.availableBeds()
        );
    }


    // =========================================================
    // TEST 6
    // EXPLICIT ROUTING ALGORITHM MUST BE USED
    // =========================================================

    @Test
    void shouldUseExplicitRoutingAlgorithm() {

        when(
                emergency.getId()
        ).thenReturn(
                1L
        );

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergency.getFacility()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                emergencyNode
        );

        Hospital hospital =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        when(
                hospital.getHospitalCode()
        ).thenReturn(
                "HOS-01"
        );

        when(
                hospital.getName()
        ).thenReturn(
                "Test Hospital"
        );

        when(
                hospital.getFacilityType()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                hospital.getAvailableBeds()
        ).thenReturn(
                10
        );

        when(
                hospital.getLatitude()
        ).thenReturn(
                28.4600
        );

        when(
                hospital.getLongitude()
        ).thenReturn(
                77.0300
        );

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(hospital)
        );

        when(
                roadGraph.findNearestNode(
                        28.4600,
                        77.0300
                )
        ).thenReturn(
                hospitalNode1
        );

        TrafficDijkstraResponse route =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(
                route.distanceKm()
        ).thenReturn(
                4.0
        );

        when(
                route.estimatedTravelTimeMinutes()
        ).thenReturn(
                7.0
        );

        when(
                route.trafficLevel()
        ).thenReturn(
                "LOW"
        );

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-1",
                        RoutingAlgorithm.ASTAR
                )
        ).thenReturn(
                route
        );

        HospitalSelectionResponse result =
                hospitalSelectionService.findBestHospital(
                        1L,
                        RoutingAlgorithm.ASTAR
                );

        assertNotNull(result);

        assertEquals(
                "HOS-01",
                result.hospitalCode()
        );

        assertEquals(
                "Test Hospital",
                result.hospitalName()
        );

        verify(
                routingService
        ).findRoute(
                "emergency-node",
                "hospital-node-1",
                RoutingAlgorithm.ASTAR
        );
    }


    // =========================================================
    // TEST 7
    // NULL ROUTING ALGORITHM MUST BE REJECTED
    // =========================================================

    @Test
    void shouldRejectNullRoutingAlgorithm() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        hospitalSelectionService
                                .findBestHospital(
                                        1L,
                                        null
                                )
        );

        verify(
                emergencyRepository,
                never()
        ).findById(1L);
    }


    // =========================================================
    // TEST 8
    // MISSING EMERGENCY SHOULD PRODUCE TYPED EXCEPTION
    // =========================================================

    @Test
    void shouldThrowExceptionWhenEmergencyDoesNotExist() {

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        when(
                emergencyRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                EmergencyNotFoundException.class,
                () ->
                        hospitalSelectionService
                                .findBestHospital(999L)
        );

        verify(
                hospitalRepository,
                never()
        ).findAll();

        verify(
                roadGraph,
                never()
        ).findNearestNode(
                anyDouble(),
                anyDouble()
        );
    }


    // =========================================================
    // TEST 9
    // EMERGENCY WITH NO ROAD NODE MUST BE REJECTED
    // =========================================================

    @Test
    void shouldRejectEmergencyWhenNoRoadNodeExists() {

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                null
        );

        assertThrows(
                NoSuitableHospitalException.class,
                () ->
                        hospitalSelectionService
                                .findBestHospital(1L)
        );

        verify(
                hospitalRepository,
                never()
        ).findAll();
    }


    // =========================================================
    // TEST 10
    // INVALID HOSPITAL COORDINATES SHOULD BE SKIPPED
    // =========================================================

    @Test
    void shouldSkipHospitalWithInvalidCoordinates() {

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergency.getFacility()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                emergencyNode
        );

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        Hospital invalidHospital =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        when(
                invalidHospital.getHospitalCode()
        ).thenReturn(
                "HOS-01"
        );

        when(
                invalidHospital.getFacilityType()
        ).thenReturn(
                "TRAUMA"
        );

        when(
                invalidHospital.getAvailableBeds()
        ).thenReturn(
                10
        );

        when(
                invalidHospital.getLatitude()
        ).thenReturn(
                200.0
        );

        when(
                invalidHospital.getLongitude()
        ).thenReturn(
                77.0300
        );

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(
                        invalidHospital
                )
        );

        when(
                roadGraph.findNearestNode(
                        200.0,
                        77.0300
                )
        ).thenReturn(
                null
        );

        assertThrows(
                NoSuitableHospitalException.class,
                () ->
                        hospitalSelectionService
                                .findBestHospital(1L)
        );

        verify(
                routingService,
                never()
        ).findRoute(
                anyString(),
                anyString(),
                any(RoutingAlgorithm.class)
        );
    }


    // =========================================================
    // TEST 11
    // NULL AVAILABLE BEDS SHOULD BE IGNORED
    // =========================================================

    @Test
    void shouldIgnoreHospitalWithNullAvailableBeds() {

        when(
                emergency.getLatitude()
        ).thenReturn(
                28.4595
        );

        when(
                emergency.getLongitude()
        ).thenReturn(
                77.0266
        );

        when(
                emergencyRepository.findById(1L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                roadGraph.findNearestNode(
                        28.4595,
                        77.0266
                )
        ).thenReturn(
                emergencyNode
        );

        when(
                routingSettingsService.getRoutingAlgorithm()
        ).thenReturn(
                RoutingAlgorithm.DIJKSTRA
        );

        Hospital hospitalWithNullBeds =
                org.mockito.Mockito.mock(
                        Hospital.class
                );

        when(
                hospitalWithNullBeds.getAvailableBeds()
        ).thenReturn(
                null
        );

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(
                        hospitalWithNullBeds
                )
        );

        assertThrows(
                NoSuitableHospitalException.class,
                () ->
                        hospitalSelectionService
                                .findBestHospital(1L)
        );

        verify(
                routingService,
                never()
        ).findRoute(
                anyString(),
                anyString(),
                any(RoutingAlgorithm.class)
        );
    }
}