package com.ambulanceos.service;

import com.ambulanceos.dto.HospitalSelectionResponse;
import com.ambulanceos.dto.TrafficDijkstraResponse;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.exception.EmergencyNotFoundException;
import com.ambulanceos.exception.NoSuitableHospitalException;
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

    @InjectMocks
    private HospitalSelectionService hospitalSelectionService;


    private Emergency emergency;

    private GraphNode emergencyNode;

    private GraphNode hospitalNode1;

    private GraphNode hospitalNode2;


    @BeforeEach
    void setUp() {

        emergency = org.mockito.Mockito.mock(Emergency.class);

        when(emergency.getId()).thenReturn(1L);
        when(emergency.getLatitude()).thenReturn(28.4595);
        when(emergency.getLongitude()).thenReturn(77.0266);
        when(emergency.getFacility()).thenReturn("TRAUMA");

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
        ).thenReturn(emergencyNode);
    }


    // =========================================================
    // TEST 1
    // =========================================================
    //
    // The hospital with the lowest traffic-adjusted travel time
    // should be selected.
    //
    // =========================================================

    @Test
    void shouldSelectFastestSuitableHospital() {

        Hospital hospital1 =
                org.mockito.Mockito.mock(Hospital.class);

        Hospital hospital2 =
                org.mockito.Mockito.mock(Hospital.class);

        when(hospital1.getId()).thenReturn(101L);
        when(hospital1.getHospitalCode()).thenReturn("HOS-01");
        when(hospital1.getName()).thenReturn("Hospital One");
        when(hospital1.getFacilityType()).thenReturn("TRAUMA");
        when(hospital1.getAvailableBeds()).thenReturn(10);
        when(hospital1.getLatitude()).thenReturn(28.4600);
        when(hospital1.getLongitude()).thenReturn(77.0300);

        when(hospital2.getId()).thenReturn(102L);
        when(hospital2.getHospitalCode()).thenReturn("HOS-02");
        when(hospital2.getName()).thenReturn("Hospital Two");
        when(hospital2.getFacilityType()).thenReturn("TRAUMA");
        when(hospital2.getAvailableBeds()).thenReturn(15);
        when(hospital2.getLatitude()).thenReturn(28.4500);
        when(hospital2.getLongitude()).thenReturn(77.0400);

        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(hospital1, hospital2)
        );

        when(
                roadGraph.findNearestNode(
                        28.4600,
                        77.0300
                )
        ).thenReturn(hospitalNode1);

        when(
                roadGraph.findNearestNode(
                        28.4500,
                        77.0400
                )
        ).thenReturn(hospitalNode2);


        TrafficDijkstraResponse fastRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(fastRoute.distanceKm()).thenReturn(5.0);
        when(fastRoute.estimatedTravelTimeMinutes())
                .thenReturn(8.0);
        when(fastRoute.trafficLevel())
                .thenReturn("LOW");


        TrafficDijkstraResponse slowRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(slowRoute.distanceKm()).thenReturn(8.0);
        when(slowRoute.estimatedTravelTimeMinutes())
                .thenReturn(15.0);
        when(slowRoute.trafficLevel())
                .thenReturn("MODERATE");


        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-1"
                )
        ).thenReturn(fastRoute);

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-2"
                )
        ).thenReturn(slowRoute);


        HospitalSelectionResponse result =
                hospitalSelectionService.findBestHospital(1L);


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
    // =========================================================
    //
    // Hospitals with zero or negative beds must be ignored.
    //
    // =========================================================

    @Test
    void shouldIgnoreHospitalWithNoAvailableBeds() {

        Hospital unavailableHospital =
                org.mockito.Mockito.mock(Hospital.class);

        when(unavailableHospital.getId()).thenReturn(101L);
        when(unavailableHospital.getHospitalCode())
                .thenReturn("HOS-01");
        when(unavailableHospital.getName())
                .thenReturn("Unavailable Hospital");
        when(unavailableHospital.getFacilityType())
                .thenReturn("TRAUMA");
        when(unavailableHospital.getAvailableBeds())
                .thenReturn(0);
        when(unavailableHospital.getLatitude())
                .thenReturn(28.4600);
        when(unavailableHospital.getLongitude())
                .thenReturn(77.0300);


        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(unavailableHospital)
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
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.anyString()
        );
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // A hospital with the wrong facility type must be ignored.
    //
    // Example:
    //
    // Emergency = TRAUMA
    // Hospital  = CARDIAC
    //
    // =========================================================

    @Test
    void shouldIgnoreHospitalWithWrongFacilityType() {

        Hospital cardiacHospital =
                org.mockito.Mockito.mock(Hospital.class);

        when(cardiacHospital.getId()).thenReturn(101L);
        when(cardiacHospital.getHospitalCode())
                .thenReturn("HOS-01");
        when(cardiacHospital.getName())
                .thenReturn("Cardiac Hospital");
        when(cardiacHospital.getFacilityType())
                .thenReturn("CARDIAC");
        when(cardiacHospital.getAvailableBeds())
                .thenReturn(20);
        when(cardiacHospital.getLatitude())
                .thenReturn(28.4600);
        when(cardiacHospital.getLongitude())
                .thenReturn(77.0300);


        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(cardiacHospital)
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
                org.mockito.Mockito.anyString(),
                org.mockito.Mockito.anyString()
        );
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // If routing fails for one hospital, that hospital should
    // be skipped instead of causing the entire selection to fail.
    //
    // =========================================================

    @Test
    void shouldSkipUnreachableHospital() {

        Hospital unreachableHospital =
                org.mockito.Mockito.mock(Hospital.class);

        Hospital reachableHospital =
                org.mockito.Mockito.mock(Hospital.class);


        when(unreachableHospital.getId()).thenReturn(101L);
        when(unreachableHospital.getHospitalCode())
                .thenReturn("HOS-01");
        when(unreachableHospital.getName())
                .thenReturn("Unreachable Hospital");
        when(unreachableHospital.getFacilityType())
                .thenReturn("TRAUMA");
        when(unreachableHospital.getAvailableBeds())
                .thenReturn(10);
        when(unreachableHospital.getLatitude())
                .thenReturn(28.4600);
        when(unreachableHospital.getLongitude())
                .thenReturn(77.0300);


        when(reachableHospital.getId()).thenReturn(102L);
        when(reachableHospital.getHospitalCode())
                .thenReturn("HOS-02");
        when(reachableHospital.getName())
                .thenReturn("Reachable Hospital");
        when(reachableHospital.getFacilityType())
                .thenReturn("TRAUMA");
        when(reachableHospital.getAvailableBeds())
                .thenReturn(12);
        when(reachableHospital.getLatitude())
                .thenReturn(28.4500);
        when(reachableHospital.getLongitude())
                .thenReturn(77.0400);


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
        ).thenReturn(hospitalNode1);

        when(
                roadGraph.findNearestNode(
                        28.4500,
                        77.0400
                )
        ).thenReturn(hospitalNode2);


        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-1"
                )
        ).thenThrow(
                new RuntimeException("No route available")
        );


        TrafficDijkstraResponse reachableRoute =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(reachableRoute.distanceKm())
                .thenReturn(6.5);

        when(reachableRoute.estimatedTravelTimeMinutes())
                .thenReturn(11.0);

        when(reachableRoute.trafficLevel())
                .thenReturn("MODERATE");


        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-2"
                )
        ).thenReturn(reachableRoute);


        HospitalSelectionResponse result =
                hospitalSelectionService.findBestHospital(1L);


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
    // =========================================================
    //
    // If two hospitals have exactly the same travel time,
    // the hospital with MORE available beds should win.
    //
    // This verifies the PriorityQueue secondary comparator.
    //
    // =========================================================

    @Test
    void shouldPreferHospitalWithMoreBedsWhenTravelTimeIsEqual() {

        Hospital hospitalWithFewerBeds =
                org.mockito.Mockito.mock(Hospital.class);

        Hospital hospitalWithMoreBeds =
                org.mockito.Mockito.mock(Hospital.class);


        when(hospitalWithFewerBeds.getId())
                .thenReturn(101L);

        when(hospitalWithFewerBeds.getHospitalCode())
                .thenReturn("HOS-01");

        when(hospitalWithFewerBeds.getName())
                .thenReturn("Hospital With Fewer Beds");

        when(hospitalWithFewerBeds.getFacilityType())
                .thenReturn("TRAUMA");

        when(hospitalWithFewerBeds.getAvailableBeds())
                .thenReturn(5);

        when(hospitalWithFewerBeds.getLatitude())
                .thenReturn(28.4600);

        when(hospitalWithFewerBeds.getLongitude())
                .thenReturn(77.0300);


        when(hospitalWithMoreBeds.getId())
                .thenReturn(102L);

        when(hospitalWithMoreBeds.getHospitalCode())
                .thenReturn("HOS-02");

        when(hospitalWithMoreBeds.getName())
                .thenReturn("Hospital With More Beds");

        when(hospitalWithMoreBeds.getFacilityType())
                .thenReturn("TRAUMA");

        when(hospitalWithMoreBeds.getAvailableBeds())
                .thenReturn(15);

        when(hospitalWithMoreBeds.getLatitude())
                .thenReturn(28.4500);

        when(hospitalWithMoreBeds.getLongitude())
                .thenReturn(77.0400);


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
        ).thenReturn(hospitalNode1);

        when(
                roadGraph.findNearestNode(
                        28.4500,
                        77.0400
                )
        ).thenReturn(hospitalNode2);


        TrafficDijkstraResponse route1 =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(route1.distanceKm())
                .thenReturn(5.0);

        when(route1.estimatedTravelTimeMinutes())
                .thenReturn(10.0);

        when(route1.trafficLevel())
                .thenReturn("LOW");


        TrafficDijkstraResponse route2 =
                org.mockito.Mockito.mock(
                        TrafficDijkstraResponse.class
                );

        when(route2.distanceKm())
                .thenReturn(7.0);

        when(route2.estimatedTravelTimeMinutes())
                .thenReturn(10.0);

        when(route2.trafficLevel())
                .thenReturn("LOW");


        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-1"
                )
        ).thenReturn(route1);

        when(
                routingService.findRoute(
                        "emergency-node",
                        "hospital-node-2"
                )
        ).thenReturn(route2);


        HospitalSelectionResponse result =
                hospitalSelectionService.findBestHospital(1L);


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
    // =========================================================
    //
    // Missing emergency should produce the typed exception.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenEmergencyDoesNotExist() {

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
                org.mockito.Mockito.anyDouble(),
                org.mockito.Mockito.anyDouble()
        );
    }
}