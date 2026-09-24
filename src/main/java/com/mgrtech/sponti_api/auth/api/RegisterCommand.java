package com.mgrtech.sponti_api.auth.api;

public record RegisterCommand(
        String password,
        String displayName,
        String phoneNumber,
        String timezone,
        String clientIp
) {
    public RegisterCommand(
            String password,
            String displayName,
            String phoneNumber,
            String timezone
    ) {
        this(password, displayName, phoneNumber, timezone, null);
    }
}
