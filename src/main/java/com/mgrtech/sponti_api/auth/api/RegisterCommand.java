package com.mgrtech.sponti_api.auth.api;

public record RegisterCommand(String email, String password, String displayName) {
}
