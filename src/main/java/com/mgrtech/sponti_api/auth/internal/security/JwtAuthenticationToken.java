package com.mgrtech.sponti_api.auth.internal.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;

    public JwtAuthenticationToken(Long userId, Collection<String> roles){
        super(roles.stream().map(SimpleGrantedAuthority::new).toList());
        this.principal = String.valueOf(userId);
        setAuthenticated(true);
    }

    @Override
    public @Nullable Object getCredentials() {
        return "";
    }

    @Override
    public @Nullable Object getPrincipal() {
        return principal;
    }
}
