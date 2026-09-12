package com.ambulanceos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {


    // =========================================================
    // AMBULANCE NOT FOUND
    // =========================================================

    @ExceptionHandler(
            AmbulanceNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleAmbulanceNotFound(
            AmbulanceNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "AMBULANCE_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    // =========================================================
    // EMERGENCY NOT FOUND
    // =========================================================

    @ExceptionHandler(
            EmergencyNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleEmergencyNotFound(
            EmergencyNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "EMERGENCY_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    // =========================================================
    // HOSPITAL NOT FOUND
    // =========================================================

    @ExceptionHandler(
            HospitalNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleHospitalNotFound(
            HospitalNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "HOSPITAL_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    // =========================================================
    // DISPATCH NOT FOUND
    // =========================================================

    @ExceptionHandler(
            DispatchNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleDispatchNotFound(
            DispatchNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "DISPATCH_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    // =========================================================
    // ROUTE NOT FOUND
    // =========================================================

    @ExceptionHandler(
            RouteNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleRouteNotFound(
            RouteNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "ROUTE_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    // =========================================================
    // NO AVAILABLE AMBULANCE
    // =========================================================

    @ExceptionHandler(
            NoAvailableAmbulanceException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleNoAvailableAmbulance(
            NoAvailableAmbulanceException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "NO_AVAILABLE_AMBULANCE",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    // =========================================================
    // NO SUITABLE HOSPITAL
    // =========================================================

    @ExceptionHandler(
            NoSuitableHospitalException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleNoSuitableHospital(
            NoSuitableHospitalException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "NO_SUITABLE_HOSPITAL",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    // =========================================================
    // VALIDATION ERRORS
    // =========================================================
    //
    // Handles:
    //
    // @Valid
    // @NotBlank
    // @NotNull
    // @DecimalMin
    // @DecimalMax
    //
    // =========================================================

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        String message =
                exception
                        .getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .collect(
                                Collectors.joining(
                                        ", "
                                )
                        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request.getRequestURI()
        );
    }


    // =========================================================
    // ILLEGAL ARGUMENT
    // =========================================================

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    // =========================================================
    // FALLBACK
    // =========================================================
    //
    // Last-resort handler for unexpected server errors.
    //
    // =========================================================

    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getRequestURI()
        );
    }


    // =========================================================
    // BUILD ERROR RESPONSE
    // =========================================================

    private ResponseEntity<ApiErrorResponse>
    buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path
    ) {

        ApiErrorResponse response =
                new ApiErrorResponse(

                        LocalDateTime.now(),

                        status.value(),

                        error,

                        message,

                        path
                );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}