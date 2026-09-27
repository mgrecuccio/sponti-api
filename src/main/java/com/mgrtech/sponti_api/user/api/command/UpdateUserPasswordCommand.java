package com.mgrtech.sponti_api.user.api.command;

public record UpdateUserPasswordCommand(
        Long userId,
        String passwordHash
) {
}
