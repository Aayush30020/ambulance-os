package com.ambulanceos.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemControllerTest {

    private final SystemController systemController =
            new SystemController();

    @Test
    void shouldReturnSystemStatus() {

        Map<String, Object> response =
                systemController.getSystemStatus();

        assertThat(response)
                .containsEntry("application", "AmbulanceOS")
                .containsEntry("status", "UP")
                .containsEntry(
                        "message",
                        "Emergency ambulance routing platform is running"
                );
    }
}