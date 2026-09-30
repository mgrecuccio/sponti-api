package com.mgrtech.sponti_api.sms.api.command;

import com.mgrtech.sponti_api.sms.api.OtpPurpose;

public record VerifyOtpCommand(
        Long userId,
        String phoneNumber,
        String verificationId,
        String otpCode,
        OtpPurpose purpose
) {
}
