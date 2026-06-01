package com.mgrtech.sponti_api.matching.internal.exception;

public class MatchScoreBelowThresholdException extends RuntimeException {
    public MatchScoreBelowThresholdException(String message) {
        super(message);
    }
}
