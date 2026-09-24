package com.mgrtech.sponti_api.sms.internal.application.response;

public record SmsBoxOtpSendResponse(
        int code,
        String message,
        String number,
        String datetime
) {
}
