package com.mgrtech.sponti_api.auth.internal.application.command;

public record VerifyRecoveryPasswordCommand(
        String phoneNumber,
        String verificationId,
        String otpCode,
        String newPassword
) {
}