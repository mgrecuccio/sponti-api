package com.mgrtech.sponti_api.auth.internal.application.command;

public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {
}
