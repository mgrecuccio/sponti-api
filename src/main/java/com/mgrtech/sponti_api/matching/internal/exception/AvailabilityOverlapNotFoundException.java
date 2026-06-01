package com.mgrtech.sponti_api.matching.internal.exception;

public class AvailabilityOverlapNotFoundException extends RuntimeException {
    public AvailabilityOverlapNotFoundException(String message) {
        super(message);
    }
}
