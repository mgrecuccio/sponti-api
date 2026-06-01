package com.mgrtech.sponti_api.user.api.view;

import java.time.LocalTime;

public record UserMatchingPreferencesView(
        Long userId,
        String timezone,
        boolean allowChat,
        boolean allowCall,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
    public boolean matchingEnabled() {
        return allowChat || allowCall;
    }
}
