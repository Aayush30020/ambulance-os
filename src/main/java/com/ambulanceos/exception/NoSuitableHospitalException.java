package com.ambulanceos.exception;

public class NoSuitableHospitalException
        extends RuntimeException {

    public NoSuitableHospitalException() {

        super(
                "No suitable hospital found"
        );
    }

    public NoSuitableHospitalException(
            String message
    ) {

        super(message);
    }
}