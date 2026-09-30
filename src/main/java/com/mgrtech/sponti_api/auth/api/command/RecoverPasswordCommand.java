package com.mgrtech.sponti_api.auth.api.command;

public record RecoverPasswordCommand(
        String phoneNumber
) {
}
