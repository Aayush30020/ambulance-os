package com.ambulanceos.service;

import com.ambulanceos.dto.AmbulanceRequest;
import com.ambulanceos.dto.AmbulanceResponse;
import com.ambulanceos.entity.Ambulance;
import com.ambulanceos.exception.AmbulanceNotFoundException;
import com.ambulanceos.repository.AmbulanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AmbulanceServiceTest {

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @InjectMocks
    private AmbulanceService ambulanceService;

    private Ambulance ambulance;

    private AmbulanceRequest ambulanceRequest;


    @BeforeEach
    void setUp() {

        ambulanceRequest =
                new AmbulanceRequest(
                        "AMB-101",
                        28.4598,
                        77.0268,
                        "available",
                        "als"
                );


        ambulance =
                Ambulance.builder()
                        .id(101L)
                        .ambulanceNumber("AMB-101")
                        .latitude(28.4598)
                        .longitude(77.0268)
                        .status("AVAILABLE")
                        .type("ALS")
                        .build();
    }


    // =========================================================
    // TEST 1
    // =========================================================
    //
    // Creating an ambulance should return the saved ambulance
    // mapped into AmbulanceResponse.
    //
    // =========================================================

    @Test
    void shouldCreateAmbulanceSuccessfully() {

        when(
                ambulanceRepository.save(
                        any(Ambulance.class)
                )
        ).thenReturn(ambulance);


        AmbulanceResponse result =
                ambulanceService.createAmbulance(
                        ambulanceRequest
                );


        assertNotNull(result);

        assertEquals(
                101L,
                result.id()
        );

        assertEquals(
                "AMB-101",
                result.ambulanceNumber()
        );

        assertEquals(
                28.4598,
                result.latitude()
        );

        assertEquals(
                77.0268,
                result.longitude()
        );

        assertEquals(
                "AVAILABLE",
                result.status()
        );

        assertEquals(
                "ALS",
                result.type()
        );


        verify(
                ambulanceRepository
        ).save(
                any(Ambulance.class)
        );
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // createAmbulance() converts status and type to uppercase.
    //
    // Example:
    //
    // available → AVAILABLE
    // als       → ALS
    //
    // =========================================================

    @Test
    void shouldNormalizeStatusAndTypeWhenCreatingAmbulance() {

        when(
                ambulanceRepository.save(
                        any(Ambulance.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );


        ArgumentCaptor<Ambulance> captor =
                ArgumentCaptor.forClass(
                        Ambulance.class
                );


        ambulanceService.createAmbulance(
                ambulanceRequest
        );


        verify(
                ambulanceRepository
        ).save(
                captor.capture()
        );


        Ambulance savedAmbulance =
                captor.getValue();


        assertEquals(
                "AVAILABLE",
                savedAmbulance.getStatus()
        );

        assertEquals(
                "ALS",
                savedAmbulance.getType()
        );

        assertEquals(
                "AMB-101",
                savedAmbulance.getAmbulanceNumber()
        );
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // getAllAmbulances() should map every entity into an
    // AmbulanceResponse.
    //
    // =========================================================

    @Test
    void shouldGetAllAmbulances() {

        Ambulance secondAmbulance =
                Ambulance.builder()
                        .id(102L)
                        .ambulanceNumber("AMB-102")
                        .latitude(28.4675)
                        .longitude(77.0312)
                        .status("EN_ROUTE")
                        .type("ALS")
                        .build();


        when(
                ambulanceRepository.findAll()
        ).thenReturn(
                List.of(
                        ambulance,
                        secondAmbulance
                )
        );


        List<AmbulanceResponse> result =
                ambulanceService.getAllAmbulances();


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
                "AMB-101",
                result.get(0).ambulanceNumber()
        );

        assertEquals(
                "AVAILABLE",
                result.get(0).status()
        );


        assertEquals(
                102L,
                result.get(1).id()
        );

        assertEquals(
                "AMB-102",
                result.get(1).ambulanceNumber()
        );

        assertEquals(
                "EN_ROUTE",
                result.get(1).status()
        );
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // Getting an ambulance by ID should return its response.
    //
    // =========================================================

    @Test
    void shouldGetAmbulanceById() {

        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        AmbulanceResponse result =
                ambulanceService.getAmbulanceById(
                        101L
                );


        assertNotNull(result);

        assertEquals(
                101L,
                result.id()
        );

        assertEquals(
                "AMB-101",
                result.ambulanceNumber()
        );

        assertEquals(
                28.4598,
                result.latitude()
        );

        assertEquals(
                77.0268,
                result.longitude()
        );

        assertEquals(
                "AVAILABLE",
                result.status()
        );

        assertEquals(
                "ALS",
                result.type()
        );


        verify(
                ambulanceRepository
        ).findById(101L);
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // Missing ambulance should throw the typed exception.
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
                        ambulanceService.getAmbulanceById(
                                999L
                        )
        );
    }


    // =========================================================
    // TEST 6
    // =========================================================
    //
    // AVAILABLE → EN_ROUTE is a valid transition.
    //
    // =========================================================

    @Test
    void shouldUpdateAmbulanceFromAvailableToEnRoute() {

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


        AmbulanceResponse result =
                ambulanceService.updateStatus(
                        101L,
                        "en_route"
                );


        assertNotNull(result);

        assertEquals(
                "EN_ROUTE",
                result.status()
        );

        assertEquals(
                "EN_ROUTE",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository
        ).save(ambulance);
    }


    // =========================================================
    // TEST 7
    // =========================================================
    //
    // EN_ROUTE → AT_EMERGENCY is a valid transition.
    //
    // =========================================================

    @Test
    void shouldUpdateAmbulanceFromEnRouteToAtEmergency() {

        ambulance.setStatus("EN_ROUTE");


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


        AmbulanceResponse result =
                ambulanceService.updateStatus(
                        101L,
                        "at_emergency"
                );


        assertNotNull(result);

        assertEquals(
                "AT_EMERGENCY",
                result.status()
        );

        assertEquals(
                "AT_EMERGENCY",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository
        ).save(ambulance);
    }


    // =========================================================
    // TEST 8
    // =========================================================
    //
    // AT_EMERGENCY → TO_HOSPITAL is a valid transition.
    //
    // =========================================================

    @Test
    void shouldUpdateAmbulanceFromAtEmergencyToHospital() {

        ambulance.setStatus("AT_EMERGENCY");


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


        AmbulanceResponse result =
                ambulanceService.updateStatus(
                        101L,
                        "to_hospital"
                );


        assertNotNull(result);

        assertEquals(
                "TO_HOSPITAL",
                result.status()
        );

        assertEquals(
                "TO_HOSPITAL",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository
        ).save(ambulance);
    }


    // =========================================================
    // TEST 9
    // =========================================================
    //
    // TO_HOSPITAL → AVAILABLE is a valid transition.
    //
    // =========================================================

    @Test
    void shouldUpdateAmbulanceFromHospitalToAvailable() {

        ambulance.setStatus("TO_HOSPITAL");


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


        AmbulanceResponse result =
                ambulanceService.updateStatus(
                        101L,
                        "available"
                );


        assertNotNull(result);

        assertEquals(
                "AVAILABLE",
                result.status()
        );

        assertEquals(
                "AVAILABLE",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository
        ).save(ambulance);
    }


    // =========================================================
    // TEST 10
    // =========================================================
    //
    // Active ambulance can be released directly to AVAILABLE.
    //
    // This is useful when a dispatch is completed/cancelled.
    //
    // =========================================================

    @Test
    void shouldAllowActiveAmbulanceToReturnToAvailable() {

        ambulance.setStatus("EN_ROUTE");


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


        AmbulanceResponse result =
                ambulanceService.updateStatus(
                        101L,
                        "AVAILABLE"
                );


        assertNotNull(result);

        assertEquals(
                "AVAILABLE",
                result.status()
        );

        assertEquals(
                "AVAILABLE",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository
        ).save(ambulance);
    }


    // =========================================================
    // TEST 11
    // =========================================================
    //
    // Setting the same status should be harmless and should
    // not unnecessarily save the entity.
    //
    // =========================================================

    @Test
    void shouldAllowSameStatusWithoutSaving() {

        ambulance.setStatus("AVAILABLE");


        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        AmbulanceResponse result =
                ambulanceService.updateStatus(
                        101L,
                        "available"
                );


        assertNotNull(result);

        assertEquals(
                "AVAILABLE",
                result.status()
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 12
    // =========================================================
    //
    // Empty status should be rejected.
    //
    // =========================================================

    @Test
    void shouldRejectEmptyAmbulanceStatus() {

        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.updateStatus(
                                101L,
                                ""
                        )
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 13
    // =========================================================
    //
    // Null status should also be rejected.
    //
    // =========================================================

    @Test
    void shouldRejectNullAmbulanceStatus() {

        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.updateStatus(
                                101L,
                                null
                        )
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 14
    // =========================================================
    //
    // Unknown status should be rejected.
    //
    // Example:
    //
    // AVAILABLE → FLYING
    //
    // =========================================================

    @Test
    void shouldRejectUnknownAmbulanceStatus() {

        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.updateStatus(
                                101L,
                                "flying"
                        )
        );


        assertEquals(
                "AVAILABLE",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 15
    // =========================================================
    //
    // Invalid transition:
    //
    // AVAILABLE → TO_HOSPITAL
    //
    // =========================================================

    @Test
    void shouldRejectInvalidStatusTransition() {

        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                ambulanceService.updateStatus(
                                        101L,
                                        "TO_HOSPITAL"
                                )
                );


        assertEquals(
                "Invalid ambulance status transition: AVAILABLE -> TO_HOSPITAL",
                exception.getMessage()
        );


        assertEquals(
                "AVAILABLE",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 16
    // =========================================================
    //
    // Invalid transition:
    //
    // EN_ROUTE → TO_HOSPITAL
    //
    // Ambulance must reach the emergency first.
    //
    // =========================================================

    @Test
    void shouldRejectSkippingEmergencyState() {

        ambulance.setStatus("EN_ROUTE");


        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.updateStatus(
                                101L,
                                "TO_HOSPITAL"
                        )
        );


        assertEquals(
                "EN_ROUTE",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 17
    // =========================================================
    //
    // Invalid transition:
    //
    // AT_EMERGENCY → EN_ROUTE
    //
    // =========================================================

    @Test
    void shouldRejectReturningToEnRouteFromEmergency() {

        ambulance.setStatus("AT_EMERGENCY");


        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.updateStatus(
                                101L,
                                "EN_ROUTE"
                        )
        );


        assertEquals(
                "AT_EMERGENCY",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 18
    // =========================================================
    //
    // Invalid transition:
    //
    // TO_HOSPITAL → EN_ROUTE
    //
    // =========================================================

    @Test
    void shouldRejectReturningToEnRouteFromHospital() {

        ambulance.setStatus("TO_HOSPITAL");


        when(
                ambulanceRepository.findById(101L)
        ).thenReturn(
                Optional.of(ambulance)
        );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.updateStatus(
                                101L,
                                "EN_ROUTE"
                        )
        );


        assertEquals(
                "TO_HOSPITAL",
                ambulance.getStatus()
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 19
    // =========================================================
    //
    // Updating the status of a missing ambulance should throw
    // AmbulanceNotFoundException.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUpdatingMissingAmbulance() {

        when(
                ambulanceRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );


        assertThrows(
                AmbulanceNotFoundException.class,
                () ->
                        ambulanceService.updateStatus(
                                999L,
                                "AVAILABLE"
                        )
        );
    }


    // =========================================================
    // TEST 20
    // =========================================================
    //
    // Creating an ambulance with an invalid status should fail.
    //
    // =========================================================

    @Test
    void shouldRejectInvalidStatusWhenCreatingAmbulance() {

        AmbulanceRequest invalidRequest =
                new AmbulanceRequest(
                        "AMB-102",
                        28.4598,
                        77.0268,
                        "flying",
                        "ALS"
                );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.createAmbulance(
                                invalidRequest
                        )
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 21
    // =========================================================
    //
    // Creating an ambulance with a null status should fail.
    //
    // =========================================================

    @Test
    void shouldRejectNullStatusWhenCreatingAmbulance() {

        AmbulanceRequest invalidRequest =
                new AmbulanceRequest(
                        "AMB-102",
                        28.4598,
                        77.0268,
                        null,
                        "ALS"
                );


        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.createAmbulance(
                                invalidRequest
                        )
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }


    // =========================================================
    // TEST 22
    // =========================================================
    //
    // Creating an ambulance with a null request should fail.
    //
    // =========================================================

    @Test
    void shouldRejectNullAmbulanceRequest() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ambulanceService.createAmbulance(
                                null
                        )
        );


        verify(
                ambulanceRepository,
                never()
        ).save(any(Ambulance.class));
    }
}