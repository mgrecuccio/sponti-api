package com.mgrtech.sponti_api.shared.error;

public class ExpiredRefreshTokenException extends InvalidRefreshTokenException {
    public ExpiredRefreshTokenException(String message) {
        super(message);
    }
}
