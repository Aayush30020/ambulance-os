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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    // CREATE TESTS
    // =========================================================

    @Test
    void shouldCreateEmergencySuccessfully() {

        when(
                emergencyRepository.save(any(Emergency.class))
        ).thenReturn(emergency);

        EmergencyResponse result =
                emergencyService.createEmergency(
                        emergencyRequest
                );

        assertNotNull(result);

        assertEquals(101L, result.id());
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
        ).save(any(Emergency.class));
    }


    @Test
    void shouldCreateEmergencyWithActiveStatus() {

        when(
                emergencyRepository.save(any(Emergency.class))
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
        ).save(captor.capture());

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


    @Test
    void shouldRejectNullEmergencyRequest() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        emergencyService.createEmergency(
                                null
                        )
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    // =========================================================
    // READ TESTS
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

        assertEquals(2, result.size());

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
    // STATUS TRANSITION TESTS
    // =========================================================

    @Test
    void shouldMoveEmergencyFromActiveToResponding() {

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


    @Test
    void shouldMoveEmergencyFromRespondingToCompleted() {

        emergency.setStatus("RESPONDING");

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
                        "completed"
                );

        assertEquals(
                "COMPLETED",
                result.status()
        );

        assertEquals(
                "COMPLETED",
                emergency.getStatus()
        );

        verify(
                emergencyRepository
        ).save(emergency);
    }


    @Test
    void shouldAllowRespondingToActiveReset() {

        emergency.setStatus("RESPONDING");

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
                        "active"
                );

        assertEquals(
                "ACTIVE",
                result.status()
        );

        assertEquals(
                "ACTIVE",
                emergency.getStatus()
        );

        verify(
                emergencyRepository
        ).save(emergency);
    }


    @Test
    void shouldAcceptStatusWithWhitespaceAndMixedCase() {

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
                        "  responding  "
                );

        assertEquals(
                "RESPONDING",
                result.status()
        );

        assertEquals(
                "RESPONDING",
                emergency.getStatus()
        );
    }


    @Test
    void shouldAllowIdempotentStatusUpdate() {

        when(
                emergencyRepository.findById(101L)
        ).thenReturn(
                Optional.of(emergency)
        );

        EmergencyResponse result =
                emergencyService.updateStatus(
                        101L,
                        "active"
                );

        assertNotNull(result);

        assertEquals(
                "ACTIVE",
                result.status()
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    // =========================================================
    // INVALID STATUS TESTS
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

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    @Test
    void shouldRejectBlankEmergencyStatus() {

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
                                "   "
                        )
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


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

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    @Test
    void shouldRejectUnknownEmergencyStatus() {

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
                                "CANCELLED"
                        )
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    // =========================================================
    // INVALID TRANSITION TESTS
    // =========================================================

    @Test
    void shouldRejectActiveToCompleted() {

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
                                "COMPLETED"
                        )
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    @Test
    void shouldRejectActiveToActiveThroughSave() {

        when(
                emergencyRepository.findById(101L)
        ).thenReturn(
                Optional.of(emergency)
        );

        EmergencyResponse result =
                emergencyService.updateStatus(
                        101L,
                        "ACTIVE"
                );

        assertEquals(
                "ACTIVE",
                result.status()
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    @Test
    void shouldRejectCompletedToActive() {

        emergency.setStatus("COMPLETED");

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
                                "ACTIVE"
                        )
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    @Test
    void shouldRejectCompletedToResponding() {

        emergency.setStatus("COMPLETED");

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
                                "RESPONDING"
                        )
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    @Test
    void shouldRejectRespondingToRespondingThroughSave() {

        emergency.setStatus("RESPONDING");

        when(
                emergencyRepository.findById(101L)
        ).thenReturn(
                Optional.of(emergency)
        );

        EmergencyResponse result =
                emergencyService.updateStatus(
                        101L,
                        "RESPONDING"
                );

        assertEquals(
                "RESPONDING",
                result.status()
        );

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }


    // =========================================================
    // MISSING EMERGENCY TEST
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

        verify(
                emergencyRepository,
                never()
        ).save(any(Emergency.class));
    }
}