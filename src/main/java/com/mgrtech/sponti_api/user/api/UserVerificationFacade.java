package com.mgrtech.sponti_api.user.api;

import com.mgrtech.sponti_api.user.api.command.VerifyUserPhoneCommand;

public interface UserVerificationFacade {

    void verify(VerifyUserPhoneCommand command);
}
