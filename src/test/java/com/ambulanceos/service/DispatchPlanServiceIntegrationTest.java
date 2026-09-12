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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DispatchPlanServiceIntegrationTest {

    @Autowired
    private DispatchPlanService dispatchPlanService;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DispatchRepository dispatchRepository;


    @Test
    void shouldCreateCompleteDispatchPlan() {

        // ---------------------------------------------------------
        // 1. Create a temporary ambulance
        // ---------------------------------------------------------
        Ambulance ambulance = ambulanceRepository.saveAndFlush(
                Ambulance.builder()
                        .ambulanceNumber("TEST-AMB-001")
                        .latitude(28.4598)
                        .longitude(77.0268)
                        .status("AVAILABLE")
                        .type("ALS")
                        .build()
        );

        // ---------------------------------------------------------
        // 2. Create a temporary emergency
        // ---------------------------------------------------------
        Emergency emergency = emergencyRepository.saveAndFlush(
                Emergency.builder()
                        .location("Integration Test Emergency")
                        .latitude(28.4595)
                        .longitude(77.0266)
                        .priority("HIGH")
                        .facility("TRAUMA")
                        .notes("Integration test emergency")
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // ---------------------------------------------------------
        // 3. Create a temporary hospital
        // ---------------------------------------------------------
        Hospital hospital = hospitalRepository.saveAndFlush(
                Hospital.builder()
                        .hospitalCode("TEST-HOS-001")
                        .name("Integration Test Hospital")
                        .latitude(28.4352)
                        .longitude(77.0817)
                        .facilityType("TRAUMA")
                        .availableBeds(10)
                        .build()
        );

        // ---------------------------------------------------------
        // 4. Create the complete dispatch plan
        // ---------------------------------------------------------
        DispatchPlanResponse response =
                dispatchPlanService.createDispatchPlan(emergency.getId());

        // ---------------------------------------------------------
        // 5. Verify top-level response
        // ---------------------------------------------------------
        assertNotNull(response);

        assertNotNull(response.dispatchId());
        assertEquals(emergency.getId(), response.emergencyId());

        // ---------------------------------------------------------
        // 6. Verify ambulance details
        // ---------------------------------------------------------
        assertNotNull(response.ambulance());
        assertNotNull(response.ambulance().id());
        assertNotNull(response.ambulance().ambulanceNumber());
        assertNotNull(response.ambulance().type());
        assertNotNull(response.ambulance().status());
        assertNotNull(response.ambulance().trafficLevel());

        assertTrue(response.ambulance().distanceKm() >= 0);
        assertTrue(response.ambulance().estimatedTravelTimeMinutes() >= 0);

        // Selected ambulance should now be en route.
        assertEquals("EN_ROUTE", response.ambulance().status());

        // ---------------------------------------------------------
        // 7. Verify hospital details
        // ---------------------------------------------------------
        assertNotNull(response.hospital());
        assertNotNull(response.hospital().id());
        assertNotNull(response.hospital().hospitalCode());
        assertNotNull(response.hospital().name());
        assertNotNull(response.hospital().facilityType());
        assertNotNull(response.hospital().trafficLevel());

        assertTrue(response.hospital().availableBeds() >= 0);
        assertTrue(response.hospital().distanceKm() >= 0);
        assertTrue(response.hospital().estimatedTravelTimeMinutes() >= 0);

        // ---------------------------------------------------------
        // 8. Verify primary route details
        // ---------------------------------------------------------
        assertNotNull(response.route());

        assertNotNull(response.route().sourceNode());
        assertNotNull(response.route().destinationNode());
        assertNotNull(response.route().trafficLevel());
        assertNotNull(response.route().nodes());

        assertTrue(response.route().distanceKm() >= 0);
        assertTrue(response.route().estimatedTravelTimeMinutes() >= 0);

        // A valid route contains at least one graph node.
        assertFalse(response.route().nodes().isEmpty());

        // ---------------------------------------------------------
        // 9. Verify trip information
        // ---------------------------------------------------------
        assertNotNull(response.trip());

        assertNotNull(response.trip().status());

        assertEquals(
                TripRoute.TripStatus.TO_EMERGENCY,
                response.trip().status()
        );

        // ---------------------------------------------------------
        // 10. Verify ambulance -> emergency route segment
        // ---------------------------------------------------------
        assertNotNull(response.trip().routeToEmergency());

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

        // ---------------------------------------------------------
        // 11. Verify emergency -> hospital route segment
        // ---------------------------------------------------------
        assertNotNull(response.trip().routeToHospital());

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

        // ---------------------------------------------------------
        // 12. Verify dispatch was persisted
        // ---------------------------------------------------------
        Dispatch savedDispatch = dispatchRepository
                .findById(response.dispatchId())
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

        assertNotNull(savedDispatch.getRoutingAlgorithm());

        assertTrue(
                savedDispatch.getTotalDistanceKm() >= 0
        );

        assertTrue(
                savedDispatch.getTotalEstimatedTimeMinutes() >= 0
        );

        assertNotNull(savedDispatch.getDispatchedAt());

        // ---------------------------------------------------------
        // 13. Verify ambulance state in database
        // ---------------------------------------------------------
        Ambulance updatedAmbulance = ambulanceRepository
                .findById(response.ambulance().id())
                .orElseThrow();

        assertEquals(
                "EN_ROUTE",
                updatedAmbulance.getStatus()
        );
    }


    @Test
    void shouldCompleteRealDispatchAndReleaseAmbulance() {

        // ---------------------------------------------------------
        // 1. Create temporary ambulance
        // ---------------------------------------------------------
        Ambulance ambulance = ambulanceRepository.saveAndFlush(
                Ambulance.builder()
                        .ambulanceNumber("TEST-AMB-002")
                        .latitude(28.4598)
                        .longitude(77.0268)
                        .status("AVAILABLE")
                        .type("ALS")
                        .build()
        );

        // ---------------------------------------------------------
        // 2. Create temporary emergency
        // ---------------------------------------------------------
        Emergency emergency = emergencyRepository.saveAndFlush(
                Emergency.builder()
                        .location("Integration Test Emergency 2")
                        .latitude(28.4595)
                        .longitude(77.0266)
                        .priority("HIGH")
                        .facility("TRAUMA")
                        .notes("Integration test emergency")
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // ---------------------------------------------------------
        // 3. Create temporary hospital
        // ---------------------------------------------------------
        hospitalRepository.saveAndFlush(
                Hospital.builder()
                        .hospitalCode("TEST-HOS-002")
                        .name("Integration Test Hospital 2")
                        .latitude(28.4352)
                        .longitude(77.0817)
                        .facilityType("TRAUMA")
                        .availableBeds(10)
                        .build()
        );

        // ---------------------------------------------------------
        // 4. Create a real dispatch plan
        // ---------------------------------------------------------
        DispatchPlanResponse response =
                dispatchPlanService.createDispatchPlan(emergency.getId());

        assertNotNull(response);
        assertNotNull(response.dispatchId());
        assertNotNull(response.ambulance());

        // ---------------------------------------------------------
        // 5. Verify dispatch is active
        // ---------------------------------------------------------
        Dispatch activeDispatch = dispatchRepository
                .findById(response.dispatchId())
                .orElseThrow();

        assertEquals(
                "IN_PROGRESS",
                activeDispatch.getStatus()
        );

        Ambulance enRouteAmbulance = ambulanceRepository
                .findById(response.ambulance().id())
                .orElseThrow();

        assertEquals(
                "EN_ROUTE",
                enRouteAmbulance.getStatus()
        );

        // ---------------------------------------------------------
        // 6. Complete the real dispatch
        // ---------------------------------------------------------
        Dispatch completedDispatch =
                dispatchService.completeDispatch(
                        response.dispatchId()
                );

        // ---------------------------------------------------------
        // 7. Verify dispatch is completed
        // ---------------------------------------------------------
        assertNotNull(completedDispatch);

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

        // ---------------------------------------------------------
        // 8. Verify ambulance was released
        // ---------------------------------------------------------
        Ambulance releasedAmbulance = ambulanceRepository
                .findById(response.ambulance().id())
                .orElseThrow();

        assertEquals(
                "AVAILABLE",
                releasedAmbulance.getStatus()
        );

        // ---------------------------------------------------------
        // 9. Verify final database state
        // ---------------------------------------------------------
        Dispatch persistedCompletedDispatch =
                dispatchRepository
                        .findById(response.dispatchId())
                        .orElseThrow();

        assertEquals(
                "COMPLETED",
                persistedCompletedDispatch.getStatus()
        );

        assertNotNull(
                persistedCompletedDispatch.getCompletedAt()
        );
    }
}