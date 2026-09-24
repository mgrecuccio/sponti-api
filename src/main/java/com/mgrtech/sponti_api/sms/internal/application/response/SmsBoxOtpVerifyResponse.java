package com.mgrtech.sponti_api.sms.internal.application.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SmsBoxOtpVerifyResponse(
        int code,
        String message,
        String number,
        @JsonProperty("client_reference")
        String clientReference,
        String datetime
) {
}
