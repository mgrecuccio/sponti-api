package com.mgrtech.sponti_api.auth.internal.application.command;

public record RecoverPasswordCommand(
        String phoneNumber,
        String clientIp
) {
}
