package com.mgrtech.sponti_api.auth.internal.application.command;

public record LoginCommand(String phoneNumber, String password) {
}
