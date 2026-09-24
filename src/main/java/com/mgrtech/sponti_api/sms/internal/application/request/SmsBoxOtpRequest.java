package com.mgrtech.sponti_api.sms.internal.application.request;

public record SmsBoxOtpRequest(
        String number,
        String text
) {
}
