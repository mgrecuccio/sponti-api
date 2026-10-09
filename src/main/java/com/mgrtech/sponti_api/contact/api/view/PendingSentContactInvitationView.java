package com.mgrtech.sponti_api.contact.api.view;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Pending sent contact invitation for the authenticated user.")
public record PendingSentContactInvitationView(
        @Schema(description = "Invitation id used for cancel actions.", example = "110")
        Long invitationId,
        @Schema(description = "Internal recipient user id.", example = "18")
        Long recipientUserId,
        @Schema(description = "Recipient E.164 phone number.", example = "+32468009911")
        String recipientPhoneNumber,
        @Schema(description = "Recipient display name.", example = "Recipient")
        String recipientDisplayName,
        @Schema(description = "Nickname chosen by the sender for this invitation.", example = "Gym buddy")
        String nickName,
        @Schema(description = "Invitation status.", example = "PENDING")
        String status,
        @Schema(description = "Invitation creation timestamp.", example = "2026-06-12T12:00:00Z")
        Instant createdAt
) {
}
