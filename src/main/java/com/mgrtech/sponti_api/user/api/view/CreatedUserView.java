package com.mgrtech.sponti_api.user.api.view;

public record CreatedUserView(
        Long id,
        String email,
        String displayName,
        String status
) {
}
