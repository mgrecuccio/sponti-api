package com.mgrtech.sponti_api.auth.api;

import com.mgrtech.sponti_api.auth.api.command.LoginCommand;
import com.mgrtech.sponti_api.auth.api.command.RegisterCommand;
import com.mgrtech.sponti_api.auth.api.dto.AuthTokens;

public interface AuthFacade {

    AuthTokens register(RegisterCommand command);

    AuthTokens login(LoginCommand command);
}
