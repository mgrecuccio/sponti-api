package com.mgrtech.sponti_api.contact.api;

import java.time.Instant;

public record ContactInvitationView(
        Long id,
        Long senderUserId,
        Long recipientUserId,
        String nickName,
        String status,
        Instant createdAt
) {
}
