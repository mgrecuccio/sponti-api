package com.mgrtech.sponti_api.shared.error;

public class UserPreferencesNotFoundException extends RuntimeException {
    public UserPreferencesNotFoundException(String message) {
        super(message);
    }
}
