package com.mgrtech.sponti_api.user.api.command;

public record CreateUserCommand(
        String email,
        String passwordHash,
        String displayName,
        String timezone
) {
}
