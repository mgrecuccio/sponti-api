package com.mgrtech.sponti_api.user.api.view;

public record UserCredentialsView(
        Long id,
        String email,
        String passwordHash
) {
}
