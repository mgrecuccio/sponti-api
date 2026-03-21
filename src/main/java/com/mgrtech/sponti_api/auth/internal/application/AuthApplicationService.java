package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.command.LoginCommand;
import com.mgrtech.sponti_api.auth.api.command.RegisterCommand;
import com.mgrtech.sponti_api.auth.api.dto.AuthTokens;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import org.springframework.stereotype.Service;

@Service
class AuthApplicationService implements AuthFacade {

    private final JwtTokenService jwtTokenService;

    AuthApplicationService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public AuthTokens register(RegisterCommand command) {
        return new AuthTokens(jwtTokenService.issueAccessToken(1L, command.email()), "refresh-token-placeholder");
    }

    @Override
    public AuthTokens login(LoginCommand command) {
        return new AuthTokens(jwtTokenService.issueAccessToken(1L, command.email()), "refresh-token-placeholder");
    }
}
