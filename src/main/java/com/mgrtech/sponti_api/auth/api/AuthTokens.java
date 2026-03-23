package com.mgrtech.sponti_api.auth.api;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
}
