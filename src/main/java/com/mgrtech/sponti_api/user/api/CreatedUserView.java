package com.mgrtech.sponti_api.user.api;

public record CreatedUserView(
        Long id,
        String email,
        String displayName,
        String status
) {
}
