package com.ambulanceos.exception;

public class EmergencyNotFoundException
        extends RuntimeException {

    public EmergencyNotFoundException(Long id) {

        super(
                "Emergency not found with id: "
                        + id
        );
    }
}