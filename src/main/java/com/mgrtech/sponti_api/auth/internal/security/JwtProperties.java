package com.mgrtech.sponti_api.auth.internal.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenMinutes,
        long refreshTokenDays
) {
}
