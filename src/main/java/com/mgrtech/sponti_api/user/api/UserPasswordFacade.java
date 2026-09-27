package com.mgrtech.sponti_api.user.api;

import com.mgrtech.sponti_api.user.api.command.UpdateUserPasswordCommand;

public interface UserPasswordFacade {

    void updatePassword(UpdateUserPasswordCommand command);
}
