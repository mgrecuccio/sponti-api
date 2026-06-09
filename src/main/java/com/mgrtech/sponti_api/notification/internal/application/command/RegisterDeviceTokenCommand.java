package com.mgrtech.sponti_api.notification.internal.application.command;

import com.mgrtech.sponti_api.notification.internal.domain.DevicePlatform;

public record RegisterDeviceTokenCommand(
        DevicePlatform platform,
        String token,
        String deviceId,
        String appVersion
) {
}
