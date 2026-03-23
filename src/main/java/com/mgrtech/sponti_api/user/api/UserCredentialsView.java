package com.mgrtech.sponti_api.user.api;

public record UserCredentialsView(
        Long id,
        String email,
        String passwordHash
) {
}
