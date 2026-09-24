package com.mgrtech.sponti_api.sms.api.command;

public record VerifyOtpCommand(
        Long userId,
        String verificationId,
        String otpCode
) {
}
