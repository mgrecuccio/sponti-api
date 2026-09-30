package com.mgrtech.sponti_api.auth.api.command;

public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {
}
