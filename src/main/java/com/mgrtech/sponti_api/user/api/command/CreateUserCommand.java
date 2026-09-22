package com.mgrtech.sponti_api.user.api.command;

public record CreateUserCommand(
        String passwordHash,
        String displayName,
        String phoneNumber,
        String timezone
) {
}
