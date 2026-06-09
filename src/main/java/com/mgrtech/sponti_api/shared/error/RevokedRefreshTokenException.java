package com.mgrtech.sponti_api.shared.error;

public class RevokedRefreshTokenException extends InvalidRefreshTokenException {
    public RevokedRefreshTokenException(String message) {
        super(message);
    }
}
