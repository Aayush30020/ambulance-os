package com.ambulanceos.service;

import com.ambulanceos.dto.DispatchPlanResponse;
import com.ambulanceos.dto.TripRoute;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.entity.Dispatch;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.repository.AmbulanceRepository;
import com.ambulanceos.repository.DispatchRepository;
import com.ambulanceos.repository.EmergencyRepository;
import com.ambulanceos.repository.HospitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DispatchPlanServiceIntegrationTest {

    @Autowired
    private DispatchPlanService dispatchPlanService;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private RoutingSettingsService routingSettingsService;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DispatchRepository dispatchRepository;


    // =========================================================
    // SETUP
    // =========================================================
    //
    // Integration tests should not depend on the routing
    // algorithm that happened to be stored in the database.
    //
    // Force the expected production/default algorithm for these
    // tests so the assertions remain deterministic.
    //
    // =========================================================

    @BeforeEach
    void setUp() {

        routingSettingsService.setRoutingAlgorithm(
                RoutingAlgorithm.DIJKSTRA
        );
    }


    // =========================================================
    // TEST 1
    // COMPLETE DISPATCH PLAN
    // =========================================================

    @Test
    void shouldCreateCompleteDispatchPlan() {

        // =====================================================
        // CREATE TEST AMBULANCE
        // =====================================================

        Ambulance ambulance =
                ambulanceRepository.saveAndFlush(
                        Ambulance.builder()
                                .ambulanceNumber(
                                        "TEST-AMB-001"
                                )
                                .latitude(28.4598)
                                .longitude(77.0268)
                                .status("AVAILABLE")
                                .type("ALS")
                                .build()
                );


        // =====================================================
        // CREATE TEST EMERGENCY
        // =====================================================

        Emergency emergency =
                emergencyRepository.saveAndFlush(
                        Emergency.builder()
                                .location(
                                        "Integration Test Emergency"
                                )
                                .latitude(28.4595)
                                .longitude(77.0266)
                                .priority("HIGH")
                                .facility("TRAUMA")
                                .notes(
                                        "Integration test emergency"
                                )
                                .status("ACTIVE")
                                .createdAt(
                                        LocalDateTime.now()
                                )
                                .build()
                );


        // =====================================================
        // CREATE TEST HOSPITAL
        // =====================================================

        Hospital hospital =
                hospitalRepository.saveAndFlush(
                        Hospital.builder()
                                .hospitalCode(
                                        "TEST-HOS-001"
                                )
                                .name(
                                        "Integration Test Hospital"
                                )
                                .latitude(28.4352)
                                .longitude(77.0817)
                                .facilityType("TRAUMA")
                                .availableBeds(10)
                                .build()
                );


        // =====================================================
        // CREATE DISPATCH PLAN
        // =====================================================

        DispatchPlanResponse response =
                dispatchPlanService.createDispatchPlan(
                        emergency.getId()
                );


        // =====================================================
        // TOP-LEVEL RESPONSE
        // =====================================================

        assertNotNull(response);

        assertNotNull(
                response.dispatchId()
        );

        assertEquals(
                emergency.getId(),
                response.emergencyId()
        );


        // =====================================================
        // AMBULANCE
        // =====================================================

        assertNotNull(
                response.ambulance()
        );

        assertNotNull(
                response.ambulance().id()
        );

        assertNotNull(
                response.ambulance().ambulanceNumber()
        );

        assertNotNull(
                response.ambulance().type()
        );

        assertEquals(
                "EN_ROUTE",
                response.ambulance().status()
        );

        assertNotNull(
                response.ambulance().trafficLevel()
        );

        assertTrue(
                response.ambulance().distanceKm() >= 0
        );

        assertTrue(
                response.ambulance()
                        .estimatedTravelTimeMinutes() >= 0
        );


        // =====================================================
        // HOSPITAL
        // =====================================================

        assertNotNull(
                response.hospital()
        );

        assertNotNull(
                response.hospital().id()
        );

        assertNotNull(
                response.hospital().hospitalCode()
        );

        assertNotNull(
                response.hospital().name()
        );

        assertNotNull(
                response.hospital().facilityType()
        );

        assertNotNull(
                response.hospital().trafficLevel()
        );

        assertTrue(
                response.hospital().availableBeds() >= 0
        );

        assertTrue(
                response.hospital().distanceKm() >= 0
        );

        assertTrue(
                response.hospital()
                        .estimatedTravelTimeMinutes() >= 0
        );


        // =====================================================
        // PRIMARY ROUTE
        // =====================================================

        assertNotNull(
                response.route()
        );

        assertNotNull(
                response.route().sourceNode()
        );

        assertNotNull(
                response.route().destinationNode()
        );

        assertNotNull(
                response.route().trafficLevel()
        );

        assertNotNull(
                response.route().nodes()
        );

        assertTrue(
                response.route().distanceKm() >= 0
        );

        assertTrue(
                response.route()
                        .estimatedTravelTimeMinutes() >= 0
        );

        assertFalse(
                response.route().nodes().isEmpty()
        );


        // =====================================================
        // TRIP
        // =====================================================

        assertNotNull(
                response.trip()
        );

        assertEquals(
                TripRoute.TripStatus.TO_EMERGENCY,
                response.trip().status()
        );


        // =====================================================
        // AMBULANCE → EMERGENCY
        // =====================================================

        assertNotNull(
                response.trip().routeToEmergency()
        );

        assertTrue(
                response.trip()
                        .routeToEmergency()
                        .distanceKm() >= 0
        );

        assertTrue(
                response.trip()
                        .routeToEmergency()
                        .estimatedTravelTimeMinutes() >= 0
        );

        assertNotNull(
                response.trip()
                        .routeToEmergency()
                        .trafficLevel()
        );

        assertNotNull(
                response.trip()
                        .routeToEmergency()
                        .nodePath()
        );

        assertNotNull(
                response.trip()
                        .routeToEmergency()
                        .coordinates()
        );

        assertFalse(
                response.trip()
                        .routeToEmergency()
                        .nodePath()
                        .isEmpty()
        );


        // =====================================================
        // EMERGENCY → HOSPITAL
        // =====================================================

        assertNotNull(
                response.trip().routeToHospital()
        );

        assertTrue(
                response.trip()
                        .routeToHospital()
                        .distanceKm() >= 0
        );

        assertTrue(
                response.trip()
                        .routeToHospital()
                        .estimatedTravelTimeMinutes() >= 0
        );

        assertNotNull(
                response.trip()
                        .routeToHospital()
                        .trafficLevel()
        );

        assertNotNull(
                response.trip()
                        .routeToHospital()
                        .nodePath()
        );

        assertNotNull(
                response.trip()
                        .routeToHospital()
                        .coordinates()
        );

        assertFalse(
                response.trip()
                        .routeToHospital()
                        .nodePath()
                        .isEmpty()
        );


        // =====================================================
        // DISPATCH DATABASE RECORD
        // =====================================================

        Dispatch savedDispatch =
                dispatchRepository
                        .findById(
                                response.dispatchId()
                        )
                        .orElseThrow();


        assertEquals(
                emergency.getId(),
                savedDispatch.getEmergencyId()
        );

        assertEquals(
                response.ambulance().id(),
                savedDispatch.getAmbulanceId()
        );

        assertEquals(
                response.hospital().id(),
                savedDispatch.getHospitalId()
        );

        assertEquals(
                "IN_PROGRESS",
                savedDispatch.getStatus()
        );

        assertNotNull(
                savedDispatch.getRoutingAlgorithm()
        );

        assertEquals(
                "DIJKSTRA",
                savedDispatch.getRoutingAlgorithm()
        );

        assertTrue(
                savedDispatch.getTotalDistanceKm() >= 0
        );

        assertTrue(
                savedDispatch
                        .getTotalEstimatedTimeMinutes() >= 0
        );

        assertNotNull(
                savedDispatch.getDispatchedAt()
        );


        // =====================================================
        // EMERGENCY DATABASE STATE
        // =====================================================

        Emergency updatedEmergency =
                emergencyRepository
                        .findById(
                                emergency.getId()
                        )
                        .orElseThrow();


        assertEquals(
                "RESPONDING",
                updatedEmergency.getStatus()
        );


        // =====================================================
        // AMBULANCE DATABASE STATE
        // =====================================================

        Ambulance updatedAmbulance =
                ambulanceRepository
                        .findById(
                                response.ambulance().id()
                        )
                        .orElseThrow();


        assertEquals(
                "EN_ROUTE",
                updatedAmbulance.getStatus()
        );
    }


    // =========================================================
    // TEST 2
    // COMPLETE REAL DISPATCH
    // =========================================================

    @Test
    void shouldCompleteRealDispatchAndReleaseAmbulance() {

        // =====================================================
        // CREATE TEST AMBULANCE
        // =====================================================

        Ambulance ambulance =
                ambulanceRepository.saveAndFlush(
                        Ambulance.builder()
                                .ambulanceNumber(
                                        "TEST-AMB-002"
                                )
                                .latitude(28.4598)
                                .longitude(77.0268)
                                .status("AVAILABLE")
                                .type("ALS")
                                .build()
                );


        // =====================================================
        // CREATE TEST EMERGENCY
        // =====================================================

        Emergency emergency =
                emergencyRepository.saveAndFlush(
                        Emergency.builder()
                                .location(
                                        "Integration Test Emergency 2"
                                )
                                .latitude(28.4595)
                                .longitude(77.0266)
                                .priority("HIGH")
                                .facility("TRAUMA")
                                .notes(
                                        "Integration test emergency"
                                )
                                .status("ACTIVE")
                                .createdAt(
                                        LocalDateTime.now()
                                )
                                .build()
                );


        // =====================================================
        // CREATE TEST HOSPITAL
        // =====================================================

        hospitalRepository.saveAndFlush(
                Hospital.builder()
                        .hospitalCode(
                                "TEST-HOS-002"
                        )
                        .name(
                                "Integration Test Hospital 2"
                        )
                        .latitude(28.4352)
                        .longitude(77.0817)
                        .facilityType("TRAUMA")
                        .availableBeds(10)
                        .build()
        );


        // =====================================================
        // CREATE DISPATCH PLAN
        // =====================================================

        DispatchPlanResponse response =
                dispatchPlanService.createDispatchPlan(
                        emergency.getId()
                );


        assertNotNull(response);

        assertNotNull(
                response.dispatchId()
        );

        assertNotNull(
                response.ambulance()
        );


        // =====================================================
        // ACTIVE DISPATCH
        // =====================================================

        Dispatch activeDispatch =
                dispatchRepository
                        .findById(
                                response.dispatchId()
                        )
                        .orElseThrow();


        assertEquals(
                "IN_PROGRESS",
                activeDispatch.getStatus()
        );


        // =====================================================
        // EMERGENCY SHOULD NOW BE RESPONDING
        // =====================================================

        Emergency respondingEmergency =
                emergencyRepository
                        .findById(
                                emergency.getId()
                        )
                        .orElseThrow();


        assertEquals(
                "RESPONDING",
                respondingEmergency.getStatus()
        );


        // =====================================================
        // AMBULANCE SHOULD BE EN_ROUTE
        // =====================================================

        Ambulance enRouteAmbulance =
                ambulanceRepository
                        .findById(
                                response.ambulance().id()
                        )
                        .orElseThrow();


        assertEquals(
                "EN_ROUTE",
                enRouteAmbulance.getStatus()
        );


        // =====================================================
        // COMPLETE DISPATCH
        // =====================================================

        Dispatch completedDispatch =
                dispatchService.completeDispatch(
                        response.dispatchId()
                );


        assertNotNull(
                completedDispatch
        );

        assertEquals(
                response.dispatchId(),
                completedDispatch.getId()
        );

        assertEquals(
                "COMPLETED",
                completedDispatch.getStatus()
        );

        assertNotNull(
                completedDispatch.getCompletedAt()
        );


        // =====================================================
        // AMBULANCE RELEASED
        // =====================================================

        Ambulance releasedAmbulance =
                ambulanceRepository
                        .findById(
                                response.ambulance().id()
                        )
                        .orElseThrow();


        assertEquals(
                "AVAILABLE",
                releasedAmbulance.getStatus()
        );


        // =====================================================
        // FINAL DISPATCH STATE
        // =====================================================

        Dispatch persistedCompletedDispatch =
                dispatchRepository
                        .findById(
                                response.dispatchId()
                        )
                        .orElseThrow();


        assertEquals(
                "COMPLETED",
                persistedCompletedDispatch.getStatus()
        );

        assertNotNull(
                persistedCompletedDispatch
                        .getCompletedAt()
        );


        // =====================================================
        // EMERGENCY REMAINS RESPONDING
        // =====================================================
        //
        // Completing a dispatch currently releases the
        // ambulance and completes the dispatch.
        //
        // Emergency completion remains a separate lifecycle
        // operation.
        //
        // =====================================================

        Emergency finalEmergency =
                emergencyRepository
                        .findById(
                                emergency.getId()
                        )
                        .orElseThrow();


        assertEquals(
                "RESPONDING",
                finalEmergency.getStatus()
        );
    }
}