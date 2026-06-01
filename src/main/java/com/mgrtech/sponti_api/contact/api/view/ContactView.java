package com.mgrtech.sponti_api.contact.api.view;

import java.time.Instant;

public record ContactView(
        Long contactUserId,
        String nickName,
        boolean favorite,
        Instant createdAt
) {
}
