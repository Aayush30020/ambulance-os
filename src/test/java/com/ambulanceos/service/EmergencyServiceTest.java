package com.ambulanceos.service;

import com.ambulanceos.dto.EmergencyRequest;
import com.ambulanceos.dto.EmergencyResponse;
import com.ambulanceos.entity.Emergency;
import com.ambulanceos.exception.EmergencyNotFoundException;
import com.ambulanceos.repository.EmergencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmergencyServiceTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @InjectMocks
    private EmergencyService emergencyService;

    private Emergency emergency;

    private EmergencyRequest emergencyRequest;


    @BeforeEach
    void setUp() {

        emergencyRequest =
                new EmergencyRequest(
                        "Golf Course Road",
                        28.4595,
                        77.0266,
                        "HIGH",
                        "TRAUMA",
                        "Road accident"
                );


        emergency =
                Emergency.builder()
                        .id(101L)
                        .location("Golf Course Road")
                        .latitude(28.4595)
                        .longitude(77.0266)
                        .priority("HIGH")
                        .facility("TRAUMA")
                        .notes("Road accident")
                        .status("ACTIVE")
                        .createdAt(
                                LocalDateTime.of(
                                        2026,
                                        9,
                                        13,
                                        10,
                                        30
                                )
                        )
                        .build();
    }


    // =========================================================
    // TEST 1
    // =========================================================
    //
    // Creating an emergency should:
    //
    // 1. Build the Emergency entity
    // 2. Set status to ACTIVE
    // 3. Save it
    // 4. Return EmergencyResponse
    //
    // =========================================================

    @Test
    void shouldCreateEmergencySuccessfully() {

        when(
                emergencyRepository.save(
                        org.mockito.Mockito.any(Emergency.class)
                )
        ).thenReturn(emergency);


        EmergencyResponse result =
                emergencyService.createEmergency(
                        emergencyRequest
                );


        assertNotNull(result);

        assertEquals(
                101L,
                result.id()
        );

        assertEquals(
                "Golf Course Road",
                result.location()
        );

        assertEquals(
                28.4595,
                result.latitude()
        );

        assertEquals(
                77.0266,
                result.longitude()
        );

        assertEquals(
                "HIGH",
                result.priority()
        );

        assertEquals(
                "TRAUMA",
                result.facility()
        );

        assertEquals(
                "Road accident",
                result.notes()
        );

        assertEquals(
                "ACTIVE",
                result.status()
        );


        verify(
                emergencyRepository
        ).save(
                org.mockito.Mockito.any(Emergency.class)
        );
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // Verify that createEmergency actually creates an entity
    // with ACTIVE status before saving.
    //
    // =========================================================

    @Test
    void shouldCreateEmergencyWithActiveStatus() {

        when(
                emergencyRepository.save(
                        org.mockito.Mockito.any(Emergency.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );


        ArgumentCaptor<Emergency> captor =
                ArgumentCaptor.forClass(
                        Emergency.class
                );


        EmergencyResponse result =
                emergencyService.createEmergency(
                        emergencyRequest
                );


        verify(
                emergencyRepository
        ).save(
                captor.capture()
        );


        Emergency savedEmergency =
                captor.getValue();


        assertEquals(
                "ACTIVE",
                savedEmergency.getStatus()
        );

        assertEquals(
                "Golf Course Road",
                savedEmergency.getLocation()
        );

        assertEquals(
                "HIGH",
                savedEmergency.getPriority()
        );

        assertEquals(
                "TRAUMA",
                savedEmergency.getFacility()
        );

        assertEquals(
                "Road accident",
                savedEmergency.getNotes()
        );

        assertNotNull(
                savedEmergency.getCreatedAt()
        );

        assertNotNull(result);
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // Get all emergencies should map every entity into an
    // EmergencyResponse.
    //
    // =========================================================

    @Test
    void shouldGetAllEmergencies() {

        Emergency secondEmergency =
                Emergency.builder()
                        .id(102L)
                        .location("MG Road")
                        .latitude(28.4700)
                        .longitude(77.0300)
                        .priority("MEDIUM")
                        .facility("GENERAL")
                        .notes("Medical emergency")
                        .status("RESPONDING")
                        .createdAt(
                                LocalDateTime.of(
                                        2026,
                                        9,
                                        13,
                                        11,
                                        0
                                )
                        )
                        .build();


        when(
                emergencyRepository.findAll()
        ).thenReturn(
                List.of(
                        emergency,
                        secondEmergency
                )
        );


        List<EmergencyResponse> result =
                emergencyService.getAllEmergencies();


        assertNotNull(result);

        assertEquals(
                2,
                result.size()
        );


        assertEquals(
                101L,
                result.get(0).id()
        );

        assertEquals(
                "Golf Course Road",
                result.get(0).location()
        );

        assertEquals(
                "ACTIVE",
                result.get(0).status()
        );


        assertEquals(
                102L,
                result.get(1).id()
        );

        assertEquals(
                "MG Road",
                result.get(1).location()
        );

        assertEquals(
                "RESPONDING",
                result.get(1).status()
        );
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // Getting an emergency by ID should return its response.
    //
    // =========================================================

    @Test
    void shouldGetEmergencyById() {

        when(
                emergencyRepository.findById(101L)
        ).thenReturn(
                Optional.of(emergency)
        );


        EmergencyResponse result =
                emergencyService.getEmergencyById(
                        101L
                );


        assertNotNull(result);

        assertEquals(
                101L,
                result.id()
        );

        assertEquals(
                "Golf Course Road",
                result.location()
        );

        assertEquals(
                "HIGH",
                result.priority()
        );

        assertEquals(
                "TRAUMA",
                result.facility()
        );

        assertEquals(
                "ACTIVE",
                result.status()
        );


        verify(
                emergencyRepository
        ).findById(101L);
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // Missing emergency should throw the typed exception.
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
                        emergencyService.getEmergencyById(
                                999L
                        )
        );
    }


    // =========================================================
    // TEST 6
    // =========================================================
    //
    // Updating status should convert the supplied status to
    // uppercase before saving.
    //
    // Example:
    //
    // responding → RESPONDING
    //
    // =========================================================

    @Test
    void shouldUpdateEmergencyStatusToUppercase() {

        when(
                emergencyRepository.findById(101L)
        ).thenReturn(
                Optional.of(emergency)
        );

        when(
                emergencyRepository.save(emergency)
        ).thenReturn(emergency);


        EmergencyResponse result =
                emergencyService.updateStatus(
                        101L,
                        "responding"
                );


        assertNotNull(result);

        assertEquals(
                "RESPONDING",
                result.status()
        );

        assertEquals(
                "RESPONDING",
                emergency.getStatus()
        );


        verify(
                emergencyRepository
        ).save(emergency);
    }


    // =========================================================
    // TEST 7
    // =========================================================
    //
    // Null or blank status should be rejected.
    //
    // =========================================================

    @Test
    void shouldRejectEmptyEmergencyStatus() {

        when(
                emergencyRepository.findById(101L)
        ).thenReturn(
                Optional.of(emergency)
        );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        emergencyService.updateStatus(
                                101L,
                                ""
                        )
        );
    }


    // =========================================================
    // TEST 8
    // =========================================================
    //
    // Null status should also be rejected.
    //
    // =========================================================

    @Test
    void shouldRejectNullEmergencyStatus() {

        when(
                emergencyRepository.findById(101L)
        ).thenReturn(
                Optional.of(emergency)
        );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        emergencyService.updateStatus(
                                101L,
                                null
                        )
        );
    }


    // =========================================================
    // TEST 9
    // =========================================================
    //
    // Updating the status of a missing emergency should throw
    // EmergencyNotFoundException.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUpdatingMissingEmergency() {

        when(
                emergencyRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );


        assertThrows(
                EmergencyNotFoundException.class,
                () ->
                        emergencyService.updateStatus(
                                999L,
                                "COMPLETED"
                        )
        );
    }
}