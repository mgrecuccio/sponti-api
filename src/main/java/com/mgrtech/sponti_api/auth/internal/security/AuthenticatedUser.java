package com.mgrtech.sponti_api.auth.internal.security;

import java.util.List;

public record AuthenticatedUser(
        Long userId,
        String email,
        List<String> roles
) {
}
