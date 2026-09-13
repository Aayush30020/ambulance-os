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
    // CREATE TESTS
    // =========================================================

    @Test
    void shouldCreateHospitalSuccessfully() {

        when(
                hospitalRepository.save(any(Hospital.class))
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
        ).save(any(Hospital.class));
    }


    @Test
    void shouldCreateHospitalWithCorrectDetails() {

        when(
                hospitalRepository.save(any(Hospital.class))
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
        ).save(captor.capture());

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


    @Test
    void shouldNormalizeHospitalTextFieldsWhenCreating() {

        HospitalRequest request =
                new HospitalRequest(
                        "  hos-05  ",
                        "  Artemis Hospital  ",
                        28.4352,
                        77.0817,
                        20,
                        "  trauma  "
                );

        when(
                hospitalRepository.save(any(Hospital.class))
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ArgumentCaptor<Hospital> captor =
                ArgumentCaptor.forClass(
                        Hospital.class
                );

        hospitalService.createHospital(request);

        verify(
                hospitalRepository
        ).save(captor.capture());

        Hospital savedHospital =
                captor.getValue();

        assertEquals(
                "HOS-05",
                savedHospital.getHospitalCode()
        );

        assertEquals(
                "Artemis Hospital",
                savedHospital.getName()
        );

        assertEquals(
                "TRAUMA",
                savedHospital.getFacilityType()
        );

        assertEquals(
                20,
                savedHospital.getAvailableBeds()
        );
    }


    @Test
    void shouldAllowZeroBedsWhenCreatingHospital() {

        HospitalRequest request =
                new HospitalRequest(
                        "HOS-03",
                        "Test Hospital",
                        28.4000,
                        77.0000,
                        0,
                        "GENERAL"
                );

        when(
                hospitalRepository.save(any(Hospital.class))
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        HospitalResponse result =
                hospitalService.createHospital(request);

        assertNotNull(result);

        assertEquals(
                0,
                result.availableBeds()
        );
    }


    @Test
    void shouldRejectNegativeBedsWhenCreatingHospital() {

        HospitalRequest request =
                new HospitalRequest(
                        "HOS-04",
                        "Test Hospital",
                        28.4000,
                        77.0000,
                        -5,
                        "GENERAL"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                hospitalService.createHospital(
                                        request
                                )
                );

        assertEquals(
                "Available beds cannot be negative",
                exception.getMessage()
        );

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


    @Test
    void shouldRejectNullBedsWhenCreatingHospital() {

        HospitalRequest request =
                new HospitalRequest(
                        "HOS-04",
                        "Test Hospital",
                        28.4000,
                        77.0000,
                        null,
                        "GENERAL"
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                hospitalService.createHospital(
                                        request
                                )
                );

        assertEquals(
                "Available beds cannot be null",
                exception.getMessage()
        );

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


    @Test
    void shouldRejectNullHospitalRequest() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        hospitalService.createHospital(
                                null
                        )
        );

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


    @Test
    void shouldRejectBlankHospitalCode() {

        HospitalRequest request =
                new HospitalRequest(
                        "   ",
                        "Test Hospital",
                        28.4000,
                        77.0000,
                        10,
                        "GENERAL"
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        hospitalService.createHospital(
                                request
                        )
        );

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


    @Test
    void shouldRejectBlankHospitalName() {

        HospitalRequest request =
                new HospitalRequest(
                        "HOS-04",
                        "   ",
                        28.4000,
                        77.0000,
                        10,
                        "GENERAL"
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        hospitalService.createHospital(
                                request
                        )
        );

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


    @Test
    void shouldRejectBlankFacilityType() {

        HospitalRequest request =
                new HospitalRequest(
                        "HOS-04",
                        "Test Hospital",
                        28.4000,
                        77.0000,
                        10,
                        "   "
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        hospitalService.createHospital(
                                request
                        )
        );

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


    // =========================================================
    // READ TESTS
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
    // BED UPDATE TESTS
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
        ).thenReturn(hospital);

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


    @Test
    void shouldAllowZeroAvailableBeds() {

        when(
                hospitalRepository.findById(101L)
        ).thenReturn(
                Optional.of(hospital)
        );

        when(
                hospitalRepository.save(hospital)
        ).thenReturn(hospital);

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

        verify(
                hospitalRepository
        ).save(hospital);
    }


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

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


    @Test
    void shouldRejectNullAvailableBeds() {

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
                                        null
                                )
                );

        assertEquals(
                "Available beds cannot be null",
                exception.getMessage()
        );

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


    @Test
    void shouldReturnExistingHospitalWhenBedCountIsUnchanged() {

        when(
                hospitalRepository.findById(101L)
        ).thenReturn(
                Optional.of(hospital)
        );

        HospitalResponse result =
                hospitalService.updateBeds(
                        101L,
                        15
                );

        assertNotNull(result);

        assertEquals(
                15,
                result.availableBeds()
        );

        assertEquals(
                15,
                hospital.getAvailableBeds()
        );

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }


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

        verify(
                hospitalRepository,
                never()
        ).save(any(Hospital.class));
    }
}