package com.mgrtech.sponti_api.auth.internal.application.command;

public record VerifyRegistrationPhoneCommand(
        Long userId,
        String verificationId,
        String otpCode
) {
}
