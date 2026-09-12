package com.ambulanceos.exception;

public class DispatchNotFoundException
        extends RuntimeException {

    public DispatchNotFoundException(Long id) {

        super(
                "Dispatch not found with id: "
                        + id
        );
    }
}