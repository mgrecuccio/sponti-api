package com.mgrtech.sponti_api.notification.api.command;

public record SendNotificationCommand(Long userId, String title, String body) {
}
