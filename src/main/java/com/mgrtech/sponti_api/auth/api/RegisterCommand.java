package com.mgrtech.sponti_api.auth.api;

public record RegisterCommand(
        String password,
        String displayName,
        String phoneNumber,
        String timezone
) { }
