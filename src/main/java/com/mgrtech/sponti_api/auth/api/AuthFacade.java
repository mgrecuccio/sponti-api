package com.mgrtech.sponti_api.auth.api;

public interface AuthFacade {

    AuthTokens register(RegisterCommand command);

    AuthTokens login(LoginCommand command);

    AuthTokens refresh(String refreshToken);

    void changePassword(ChangePasswordCommand command);

    void logout(Long userId);
}
