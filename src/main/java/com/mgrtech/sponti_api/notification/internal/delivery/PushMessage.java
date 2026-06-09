package com.mgrtech.sponti_api.notification.internal.delivery;

import com.mgrtech.sponti_api.notification.api.NotificationType;

import java.util.Map;

public record PushMessage(
        String token,
        NotificationType type,
        String title,
        String body,
        Map<String, String> data
) {
}
