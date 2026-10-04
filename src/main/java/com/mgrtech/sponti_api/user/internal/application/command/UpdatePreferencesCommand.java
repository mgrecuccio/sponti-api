package com.mgrtech.sponti_api.user.internal.application.command;

import java.time.LocalTime;

public record UpdatePreferencesCommand(
        boolean allowChat,
        boolean allowCall,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        boolean pushNotificationsEnabled,
        boolean suggestionNotificationsEnabled
) {
}
