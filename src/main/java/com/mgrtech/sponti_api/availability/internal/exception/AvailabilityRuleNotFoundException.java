package com.mgrtech.sponti_api.availability.internal.exception;

public class AvailabilityRuleNotFoundException extends RuntimeException {
    public AvailabilityRuleNotFoundException(String message) {
        super(message);
    }
}