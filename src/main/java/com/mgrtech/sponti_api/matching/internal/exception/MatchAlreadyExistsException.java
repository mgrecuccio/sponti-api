package com.mgrtech.sponti_api.matching.internal.exception;

public class MatchAlreadyExistsException extends RuntimeException {
    public MatchAlreadyExistsException(String message) {
        super(message);
    }
}
