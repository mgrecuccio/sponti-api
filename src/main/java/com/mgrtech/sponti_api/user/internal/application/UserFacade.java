package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;
import com.mgrtech.sponti_api.user.internal.application.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.view.UserPrivateProfileView;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;
import com.mgrtech.sponti_api.user.internal.application.command.VerifyPhoneCommand;

public interface UserFacade {

    UserProfileView updateProfile(Long userId, UpdateUserCommand command);

    UserPrivateProfileView getCurrentUserProfile(Long userId);

    VerificationView resendPhoneVerification(Long userId, String clientIp);

    VerificationResultView verifyPhone(Long userId, VerifyPhoneCommand command);
}
