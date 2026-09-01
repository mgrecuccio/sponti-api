package com.mgrtech.sponti_api.contact.api.view;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Contact relationship visible to the authenticated user.")
public record ContactView(
        @Schema(description = "Internal id of the contact user.", example = "24")
        Long contactUserId,
        @Schema(description = "User-defined nickname for this contact.", example = "Gym buddy")
        String nickName,
        @Schema(description = "Contact user's profile display name.", example = "Marco")
        String displayName,
        @Schema(description = "Whether this contact is marked as favorite.", example = "true")
        boolean favorite,
        @Schema(description = "Timestamp when this contact relationship was created.", example = "2026-06-12T12:00:00Z")
        Instant createdAt
) {
        public ContactView(Long contactUserId, String nickName, boolean favorite, Instant createdAt) {
                this(contactUserId, nickName, null, favorite, createdAt);
        }
}
