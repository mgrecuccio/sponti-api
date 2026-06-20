package com.mgrtech.sponti_api.user.api.command;

public record UpdateUserCommand(
        String displayName,
        String timezone,
        String phoneNumber
) {
    public UpdateUserCommand(
            String displayName,
            String timezone
    ) {
        this(displayName, timezone, null);
    }
}
