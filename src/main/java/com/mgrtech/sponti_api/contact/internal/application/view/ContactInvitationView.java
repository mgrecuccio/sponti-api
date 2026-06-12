package com.mgrtech.sponti_api.contact.internal.application.view;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Contact invitation response.")
public record ContactInvitationView(
        @Schema(description = "Invitation id.", example = "110")
        Long id,
        @Schema(description = "Internal sender user id.", example = "42")
        Long senderUserId,
        @Schema(description = "Internal recipient user id.", example = "24")
        Long recipientUserId,
        @Schema(description = "Nickname proposed by the sender.", example = "Team mate")
        String nickName,
        @Schema(description = "Invitation status.", example = "PENDING")
        String status,
        @Schema(description = "Invitation creation timestamp.", example = "2026-06-12T12:00:00Z")
        Instant createdAt
) {
}
