package com.mgrtech.sponti_api.notification.api.command;

import com.mgrtech.sponti_api.notification.api.NotificationType;

import java.util.Map;

public record SendNotificationCommand(
        Long userId,
        NotificationType type,
        String title,
        String body,
        Map<String, String> data
) {
}
