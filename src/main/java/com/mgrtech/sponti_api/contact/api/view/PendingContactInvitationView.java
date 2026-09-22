package com.mgrtech.sponti_api.contact.api.view;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Pending incoming contact invitation for the authenticated user.")
public record PendingContactInvitationView(
        @Schema(description = "Invitation id used for accept/reject actions.", example = "110")
        Long invitationId,
        @Schema(description = "Internal sender user id.", example = "18")
        Long senderUserId,
        @Schema(description = "Sender E.164 phone number.", example = "+32468009911")
        String senderPhoneNumber,
        @Schema(description = "Sender display name.", example = "Sender")
        String senderDisplayName,
        @Schema(description = "Invitation status.", example = "PENDING")
        String status,
        @Schema(description = "Invitation creation timestamp.", example = "2026-06-12T12:00:00Z")
        Instant createdAt
) {
}
