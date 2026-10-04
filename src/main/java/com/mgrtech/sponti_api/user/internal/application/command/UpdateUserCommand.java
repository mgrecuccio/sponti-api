package com.mgrtech.sponti_api.user.internal.application.command;

public record UpdateUserCommand(
        String displayName,
        String timezone,
        String phoneNumber
) {
}
