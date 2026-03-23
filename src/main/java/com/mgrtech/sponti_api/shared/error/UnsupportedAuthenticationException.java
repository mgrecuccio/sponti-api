package com.mgrtech.sponti_api.shared.error;

public class UnsupportedAuthenticationException extends RuntimeException {
    public UnsupportedAuthenticationException(String message) {
        super(message);
    }
}
