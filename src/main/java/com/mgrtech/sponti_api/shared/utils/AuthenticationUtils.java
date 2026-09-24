package com.mgrtech.sponti_api.shared.utils;

import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import org.springframework.security.core.Authentication;

public class AuthenticationUtils {

    private AuthenticationUtils() {
    }

    public static Long extractUserId(Authentication authentication) {
        var principal = authentication.getPrincipal();

        if (principal instanceof String value) {
            return Long.valueOf(value);
        }

        throw new UnsupportedAuthenticationException("Unsupported authentication principal");
    }
}
