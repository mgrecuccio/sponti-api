package com.mgrtech.sponti_api.sms.api.command;

public record SendOtpCommand(
        Long userId,
        String phoneNumber,
        String clientIp
) {
    public SendOtpCommand(
            Long userId,
            String phoneNumber
    ) {
        this(userId, phoneNumber, null);
    }
}
