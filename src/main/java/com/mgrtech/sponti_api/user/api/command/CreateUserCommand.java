package com.mgrtech.sponti_api.user.api.command;

public record CreateUserCommand(
        String email,
        String passwordHash,
        String displayName,
        String phoneNumber,
        String timezone
) {
    public CreateUserCommand(
            String email,
            String passwordHash,
            String displayName,
            String timezone
    ) {
        this(email, passwordHash, displayName, null, timezone);
    }
}
