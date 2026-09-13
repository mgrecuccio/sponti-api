package com.mgrtech.sponti_api.matching.internal.exception;

public class MatchNotCurrentlyActiveException extends RuntimeException {
    public MatchNotCurrentlyActiveException(String message) {
        super(message);
    }
}
