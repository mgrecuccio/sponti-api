package com.mgrtech.sponti_api.user.api.view;

public record UserProfileView(
        Long id,
        String email,
        String displayName,
        String status,
        String timezone
) {
}
