package com.mgrtech.sponti_api.auth.api.command;

public record VerifyRecoveryPasswordCommand(
        String phoneNumber,
        String verificationId,
        String otpCode,
        String newPassword
) {
}