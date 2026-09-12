package com.ambulanceos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = mock(HttpServletRequest.class);
        when(request.getRequestURI())
                .thenReturn("/api/test");
    }


    // =========================================================
    // AMBULANCE NOT FOUND
    // =========================================================

    @Test
    void shouldHandleAmbulanceNotFound() {

        AmbulanceNotFoundException exception =
                new AmbulanceNotFoundException(999L);

        var response =
                handler.handleAmbulanceNotFound(
                        exception,
                        request
                );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                404,
                response.getBody().status()
        );

        assertEquals(
                "AMBULANCE_NOT_FOUND",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // EMERGENCY NOT FOUND
    // =========================================================

    @Test
    void shouldHandleEmergencyNotFound() {

        EmergencyNotFoundException exception =
                new EmergencyNotFoundException(999L);

        var response =
                handler.handleEmergencyNotFound(
                        exception,
                        request
                );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                404,
                response.getBody().status()
        );

        assertEquals(
                "EMERGENCY_NOT_FOUND",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // HOSPITAL NOT FOUND
    // =========================================================

    @Test
    void shouldHandleHospitalNotFound() {

        HospitalNotFoundException exception =
                new HospitalNotFoundException(999L);

        var response =
                handler.handleHospitalNotFound(
                        exception,
                        request
                );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                404,
                response.getBody().status()
        );

        assertEquals(
                "HOSPITAL_NOT_FOUND",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // DISPATCH NOT FOUND
    // =========================================================

    @Test
    void shouldHandleDispatchNotFound() {

        DispatchNotFoundException exception =
                new DispatchNotFoundException(999L);

        var response =
                handler.handleDispatchNotFound(
                        exception,
                        request
                );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                404,
                response.getBody().status()
        );

        assertEquals(
                "DISPATCH_NOT_FOUND",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // ROUTE NOT FOUND
    // =========================================================

    @Test
    void shouldHandleRouteNotFound() {

        RouteNotFoundException exception =
                new RouteNotFoundException(
                        "No route could be found"
                );

        var response =
                handler.handleRouteNotFound(
                        exception,
                        request
                );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                404,
                response.getBody().status()
        );

        assertEquals(
                "ROUTE_NOT_FOUND",
                response.getBody().error()
        );

        assertEquals(
                "No route could be found",
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // NO AVAILABLE AMBULANCE
    // =========================================================

    @Test
    void shouldHandleNoAvailableAmbulance() {

        NoAvailableAmbulanceException exception =
                new NoAvailableAmbulanceException(
                        "No ambulance is currently available"
                );

        var response =
                handler.handleNoAvailableAmbulance(
                        exception,
                        request
                );

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                409,
                response.getBody().status()
        );

        assertEquals(
                "NO_AVAILABLE_AMBULANCE",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // NO SUITABLE HOSPITAL
    // =========================================================

    @Test
    void shouldHandleNoSuitableHospital() {

        NoSuitableHospitalException exception =
                new NoSuitableHospitalException(
                        "No suitable hospital is available"
                );

        var response =
                handler.handleNoSuitableHospital(
                        exception,
                        request
                );

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                409,
                response.getBody().status()
        );

        assertEquals(
                "NO_SUITABLE_HOSPITAL",
                response.getBody().error()
        );

        assertEquals(
                exception.getMessage(),
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // VALIDATION ERROR
    // =========================================================

    @Test
    void shouldHandleValidationException() {

        BindingResult bindingResult =
                mock(BindingResult.class);

        FieldError locationError =
                new FieldError(
                        "emergencyRequest",
                        "location",
                        "must not be blank"
                );

        FieldError latitudeError =
                new FieldError(
                        "emergencyRequest",
                        "latitude",
                        "must not be null"
                );

        when(bindingResult.getFieldErrors())
                .thenReturn(
                        List.of(
                                locationError,
                                latitudeError
                        )
                );

        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        when(exception.getBindingResult())
                .thenReturn(bindingResult);

        var response =
                handler.handleValidationException(
                        exception,
                        request
                );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                400,
                response.getBody().status()
        );

        assertEquals(
                "VALIDATION_ERROR",
                response.getBody().error()
        );

        assertEquals(
                "location: must not be blank, latitude: must not be null",
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // ILLEGAL ARGUMENT
    // =========================================================

    @Test
    void shouldHandleIllegalArgument() {

        IllegalArgumentException exception =
                new IllegalArgumentException(
                        "Invalid status value"
                );

        var response =
                handler.handleIllegalArgument(
                        exception,
                        request
                );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                400,
                response.getBody().status()
        );

        assertEquals(
                "INVALID_REQUEST",
                response.getBody().error()
        );

        assertEquals(
                "Invalid status value",
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }


    // =========================================================
    // GENERIC / UNEXPECTED ERROR
    // =========================================================

    @Test
    void shouldHandleGenericException() {

        Exception exception =
                new RuntimeException(
                        "Unexpected database failure"
                );

        var response =
                handler.handleGenericException(
                        exception,
                        request
                );

        assertEquals(
                500,
                response.getStatusCode().value()
        );

        assertNotNull(response.getBody());

        assertEquals(
                500,
                response.getBody().status()
        );

        assertEquals(
                "INTERNAL_SERVER_ERROR",
                response.getBody().error()
        );

        // The actual exception message should NOT be exposed.
        assertEquals(
                "An unexpected error occurred",
                response.getBody().message()
        );

        assertEquals(
                "/api/test",
                response.getBody().path()
        );

        assertNotNull(response.getBody().timestamp());
    }
}