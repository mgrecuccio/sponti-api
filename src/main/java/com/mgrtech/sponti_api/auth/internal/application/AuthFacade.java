package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.auth.internal.application.view.AuthTokens;
import com.mgrtech.sponti_api.auth.internal.application.command.*;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;

public interface AuthFacade {

    AuthTokens register(RegisterCommand command);

    AuthTokens login(LoginCommand command);

    AuthTokens refresh(String refreshToken);

    void changePassword(ChangePasswordCommand command);

    void logout(Long userId);

    VerificationResultView verifyRegistrationPhone(VerifyRegistrationPhoneCommand command);

    VerificationView recoverPassword(RecoverPasswordCommand command);

    void verifyPasswordRecovery(VerifyRecoveryPasswordCommand command);

    void deleteAuthenticatedUser(Long userId);
}
