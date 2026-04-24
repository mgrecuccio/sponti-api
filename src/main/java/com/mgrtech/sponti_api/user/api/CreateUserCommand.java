package com.mgrtech.sponti_api.user.api;

public record CreateUserCommand(
        String email,
        String passwordHash,
        String displayName,
        String timezone
) {
}
