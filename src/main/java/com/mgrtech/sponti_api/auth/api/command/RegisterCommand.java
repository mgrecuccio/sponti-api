package com.mgrtech.sponti_api.auth.api.command;

public record RegisterCommand(String email, String password, String displayName) {
}
