package com.ambulanceos.exception;

public class AmbulanceNotFoundException
        extends RuntimeException {

    public AmbulanceNotFoundException(Long id) {

        super(
                "Ambulance not found with id: "
                        + id
        );
    }
}