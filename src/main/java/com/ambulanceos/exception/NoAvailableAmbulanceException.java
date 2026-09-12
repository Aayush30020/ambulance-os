package com.ambulanceos.exception;

public class NoAvailableAmbulanceException
        extends RuntimeException {

    public NoAvailableAmbulanceException() {

        super(
                "No available ambulance found"
        );
    }

    public NoAvailableAmbulanceException(
            String message
    ) {

        super(message);
    }
}