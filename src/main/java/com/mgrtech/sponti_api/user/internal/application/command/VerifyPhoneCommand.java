package com.mgrtech.sponti_api.user.internal.application.command;

public record VerifyPhoneCommand(
        String verificationId,
        String otpCode
) {
}
