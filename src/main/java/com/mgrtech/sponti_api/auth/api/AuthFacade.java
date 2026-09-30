package com.mgrtech.sponti_api.auth.api;

import com.mgrtech.sponti_api.auth.api.command.ChangePasswordCommand;
import com.mgrtech.sponti_api.auth.api.command.LoginCommand;
import com.mgrtech.sponti_api.auth.api.command.RegisterCommand;
import com.mgrtech.sponti_api.auth.api.command.VerifyRegistrationPhoneCommand;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;

public interface AuthFacade {

    AuthTokens register(RegisterCommand command);

    AuthTokens login(LoginCommand command);

    AuthTokens refresh(String refreshToken);

    void changePassword(ChangePasswordCommand command);

    void logout(Long userId);

    VerificationResultView verifyRegistrationPhone(VerifyRegistrationPhoneCommand command);
}
