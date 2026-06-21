package com.mgrtech.sponti_api.user.api.view;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authenticated private user's profile response.")
public record UserPrivateProfileView(
        @Schema(description = "Authenticated user's internal id.", example = "42")
        Long id,
        @Schema(description = "Authenticated user's normalized email address.", example = "marco@example.com")
        String email,
        @Schema(description = "Authenticated user's E.164 phone number.", example = "+32468009911")
        String phoneNumber,
        @Schema(description = "Display name shown to contacts and invitations.", example = "Marco")
        String displayName,
        @Schema(description = "User account status.", example = "ACTIVE")
        String status,
        @Schema(description = "IANA timezone used for matching and availability.", example = "Europe/Brussels")
        String timezone
) {

}
