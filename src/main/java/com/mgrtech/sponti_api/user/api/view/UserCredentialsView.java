package com.mgrtech.sponti_api.user.api.view;

public record UserCredentialsView(
        Long id,
        String status,
        String phoneNumber,
        String passwordHash
) {
}
