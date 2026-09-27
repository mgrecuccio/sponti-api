package com.mgrtech.sponti_api.auth.api;

public record ChangePasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword
) {
}
