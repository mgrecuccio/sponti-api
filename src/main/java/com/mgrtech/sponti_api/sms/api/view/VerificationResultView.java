package com.mgrtech.sponti_api.sms.api.view;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Code OTP verification feedback view response.")
public record VerificationResultView(
        Long userId,
        boolean verified
) {
}
