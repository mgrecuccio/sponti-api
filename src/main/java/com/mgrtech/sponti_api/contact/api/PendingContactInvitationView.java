package com.mgrtech.sponti_api.contact.api;

import java.time.Instant;

public record PendingContactInvitationView(
        Long invitationId,
        Long senderUserId,
        String senderEmail,
        String senderDisplayName,
        String nickName,
        String status,
        Instant createdAt
) {
}
