package com.ambulanceos.controller;

import com.ambulanceos.dto.DispatchResponse;
import com.ambulanceos.service.DispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchControllerTest {

    @Mock
    private DispatchService dispatchService;

    @InjectMocks
    private DispatchController dispatchController;


    // =========================================================
    // FIND BEST AMBULANCE
    // =========================================================

    @Test
    void shouldFindBestAmbulance() {

        Long emergencyId = 1L;

        DispatchResponse response =
                new DispatchResponse(
                        1L,
                        10L,
                        "AMB-101",
                        "BLS",
                        "AVAILABLE",
                        5.25,
                        8.50,
                        "MODERATE"
                );

        when(
                dispatchService.findBestAmbulance(
                        emergencyId
                )
        ).thenReturn(response);


        DispatchResponse result =
                dispatchController.findBestAmbulance(
                        emergencyId
                );


        assertThat(result)
                .isEqualTo(response);

        verify(
                dispatchService
        ).findBestAmbulance(
                emergencyId
        );
    }


    // =========================================================
    // DISPATCH AMBULANCE
    // =========================================================

    @Test
    void shouldDispatchAmbulance() {

        Long emergencyId = 1L;

        DispatchResponse response =
                new DispatchResponse(
                        1L,
                        10L,
                        "AMB-101",
                        "BLS",
                        "EN_ROUTE",
                        5.25,
                        8.50,
                        "MODERATE"
                );

        when(
                dispatchService.dispatchAmbulance(
                        emergencyId
                )
        ).thenReturn(response);


        DispatchResponse result =
                dispatchController.dispatchAmbulance(
                        emergencyId
                );


        assertThat(result)
                .isEqualTo(response);

        verify(
                dispatchService
        ).dispatchAmbulance(
                emergencyId
        );
    }
}