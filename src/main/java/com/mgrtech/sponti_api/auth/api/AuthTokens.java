package com.mgrtech.sponti_api.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication token response. Store refresh tokens securely on device.")
public record AuthTokens(
        @Schema(description = "Short-lived JWT access token.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Opaque refresh token used to rotate sessions.", example = "opaque-refresh-token")
        String refreshToken,
        @Schema(description = "Token type for Authorization header.", example = "Bearer")
        String tokenType,
        @Schema(description = "Access token lifetime in seconds.", example = "900")
        long expiresInSeconds
) {
}
