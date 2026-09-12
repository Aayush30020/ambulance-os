package com.ambulanceos.service;

import com.ambulanceos.dto.HospitalRequest;
import com.ambulanceos.dto.HospitalResponse;
import com.ambulanceos.entity.Hospital;
import com.ambulanceos.exception.HospitalNotFoundException;
import com.ambulanceos.repository.HospitalRepository;
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
class HospitalServiceTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @InjectMocks
    private HospitalService hospitalService;

    private Hospital hospital;

    private HospitalRequest hospitalRequest;


    @BeforeEach
    void setUp() {

        hospitalRequest =
                new HospitalRequest(
                        "HOS-01",
                        "Medanta Hospital",
                        28.4246,
                        76.9957,
                        15,
                        "TRAUMA"
                );


        hospital =
                Hospital.builder()
                        .id(101L)
                        .hospitalCode("HOS-01")
                        .name("Medanta Hospital")
                        .latitude(28.4246)
                        .longitude(76.9957)
                        .availableBeds(15)
                        .facilityType("TRAUMA")
                        .build();
    }


    // =========================================================
    // TEST 1
    // =========================================================
    //
    // Creating a hospital should save the entity and return
    // the mapped HospitalResponse.
    //
    // =========================================================

    @Test
    void shouldCreateHospitalSuccessfully() {

        when(
                hospitalRepository.save(
                        org.mockito.Mockito.any(Hospital.class)
                )
        ).thenReturn(hospital);


        HospitalResponse result =
                hospitalService.createHospital(
                        hospitalRequest
                );


        assertNotNull(result);

        assertEquals(
                101L,
                result.id()
        );

        assertEquals(
                "HOS-01",
                result.hospitalCode()
        );

        assertEquals(
                "Medanta Hospital",
                result.name()
        );

        assertEquals(
                28.4246,
                result.latitude()
        );

        assertEquals(
                76.9957,
                result.longitude()
        );

        assertEquals(
                15,
                result.availableBeds()
        );

        assertEquals(
                "TRAUMA",
                result.facilityType()
        );


        verify(
                hospitalRepository
        ).save(
                org.mockito.Mockito.any(Hospital.class)
        );
    }


    // =========================================================
    // TEST 2
    // =========================================================
    //
    // Verify that createHospital() creates the correct entity
    // before saving it.
    //
    // =========================================================

    @Test
    void shouldCreateHospitalWithCorrectDetails() {

        when(
                hospitalRepository.save(
                        org.mockito.Mockito.any(Hospital.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );


        ArgumentCaptor<Hospital> captor =
                ArgumentCaptor.forClass(
                        Hospital.class
                );


        hospitalService.createHospital(
                hospitalRequest
        );


        verify(
                hospitalRepository
        ).save(
                captor.capture()
        );


        Hospital savedHospital =
                captor.getValue();


        assertEquals(
                "HOS-01",
                savedHospital.getHospitalCode()
        );

        assertEquals(
                "Medanta Hospital",
                savedHospital.getName()
        );

        assertEquals(
                28.4246,
                savedHospital.getLatitude()
        );

        assertEquals(
                76.9957,
                savedHospital.getLongitude()
        );

        assertEquals(
                15,
                savedHospital.getAvailableBeds()
        );

        assertEquals(
                "TRAUMA",
                savedHospital.getFacilityType()
        );
    }


    // =========================================================
    // TEST 3
    // =========================================================
    //
    // getAllHospitals() should map every hospital entity into
    // a HospitalResponse.
    //
    // =========================================================

    @Test
    void shouldGetAllHospitals() {

        Hospital secondHospital =
                Hospital.builder()
                        .id(102L)
                        .hospitalCode("HOS-02")
                        .name("Artemis Hospital")
                        .latitude(28.4352)
                        .longitude(77.0817)
                        .availableBeds(11)
                        .facilityType("ICU")
                        .build();


        when(
                hospitalRepository.findAll()
        ).thenReturn(
                List.of(
                        hospital,
                        secondHospital
                )
        );


        List<HospitalResponse> result =
                hospitalService.getAllHospitals();


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
                "HOS-01",
                result.get(0).hospitalCode()
        );

        assertEquals(
                "Medanta Hospital",
                result.get(0).name()
        );

        assertEquals(
                15,
                result.get(0).availableBeds()
        );


        assertEquals(
                102L,
                result.get(1).id()
        );

        assertEquals(
                "HOS-02",
                result.get(1).hospitalCode()
        );

        assertEquals(
                "Artemis Hospital",
                result.get(1).name()
        );

        assertEquals(
                11,
                result.get(1).availableBeds()
        );
    }


    // =========================================================
    // TEST 4
    // =========================================================
    //
    // Getting a hospital by ID should return its response.
    //
    // =========================================================

    @Test
    void shouldGetHospitalById() {

        when(
                hospitalRepository.findById(101L)
        ).thenReturn(
                Optional.of(hospital)
        );


        HospitalResponse result =
                hospitalService.getHospitalById(
                        101L
                );


        assertNotNull(result);

        assertEquals(
                101L,
                result.id()
        );

        assertEquals(
                "HOS-01",
                result.hospitalCode()
        );

        assertEquals(
                "Medanta Hospital",
                result.name()
        );

        assertEquals(
                "TRAUMA",
                result.facilityType()
        );


        verify(
                hospitalRepository
        ).findById(101L);
    }


    // =========================================================
    // TEST 5
    // =========================================================
    //
    // Missing hospital should throw the typed exception.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenHospitalDoesNotExist() {

        when(
                hospitalRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );


        assertThrows(
                HospitalNotFoundException.class,
                () ->
                        hospitalService.getHospitalById(
                                999L
                        )
        );
    }


    // =========================================================
    // TEST 6
    // =========================================================
    //
    // Updating available beds should save the new value and
    // return the updated response.
    //
    // =========================================================

    @Test
    void shouldUpdateAvailableBeds() {

        when(
                hospitalRepository.findById(101L)
        ).thenReturn(
                Optional.of(hospital)
        );

        when(
                hospitalRepository.save(hospital)
        ).thenReturn(
                hospital
        );


        HospitalResponse result =
                hospitalService.updateBeds(
                        101L,
                        25
                );


        assertNotNull(result);

        assertEquals(
                25,
                result.availableBeds()
        );

        assertEquals(
                25,
                hospital.getAvailableBeds()
        );


        verify(
                hospitalRepository
        ).save(hospital);
    }


    // =========================================================
    // TEST 7
    // =========================================================
    //
    // Zero available beds is valid.
    //
    // A hospital can have zero beds available; the selection
    // service simply won't choose it.
    //
    // =========================================================

    @Test
    void shouldAllowZeroAvailableBeds() {

        when(
                hospitalRepository.findById(101L)
        ).thenReturn(
                Optional.of(hospital)
        );

        when(
                hospitalRepository.save(hospital)
        ).thenReturn(
                hospital
        );


        HospitalResponse result =
                hospitalService.updateBeds(
                        101L,
                        0
                );


        assertNotNull(result);

        assertEquals(
                0,
                result.availableBeds()
        );

        assertEquals(
                0,
                hospital.getAvailableBeds()
        );
    }


    // =========================================================
    // TEST 8
    // =========================================================
    //
    // Negative bed count should be rejected.
    //
    // =========================================================

    @Test
    void shouldRejectNegativeAvailableBeds() {

        when(
                hospitalRepository.findById(101L)
        ).thenReturn(
                Optional.of(hospital)
        );


        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                hospitalService.updateBeds(
                                        101L,
                                        -5
                                )
                );


        assertEquals(
                "Available beds cannot be negative",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST 9
    // =========================================================
    //
    // Updating beds for a missing hospital should throw the
    // typed HospitalNotFoundException.
    //
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUpdatingMissingHospital() {

        when(
                hospitalRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );


        assertThrows(
                HospitalNotFoundException.class,
                () ->
                        hospitalService.updateBeds(
                                999L,
                                10
                        )
        );
    }
}