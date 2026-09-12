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
                        org.mockito.Mockito.any(Ambulance.class)
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
                org.mockito.Mockito.any(Ambulance.class)
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
                        org.mockito.Mockito.any(Ambulance.class)
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
    // Updating the status should convert the supplied status
    // to uppercase before saving.
    //
    // =========================================================

    @Test
    void shouldUpdateAmbulanceStatusToUppercase() {

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
    }


    // =========================================================
    // TEST 8
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
    }


    // =========================================================
    // TEST 9
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
}