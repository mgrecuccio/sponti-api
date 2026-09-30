package com.mgrtech.sponti_api.auth.api.command;

public record VerifyRegistrationPhoneCommand(
        Long userId,
        String verificationId,
        String otpCode
) {
}
