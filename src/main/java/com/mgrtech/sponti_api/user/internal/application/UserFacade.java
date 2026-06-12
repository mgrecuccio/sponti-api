package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;

public interface UserFacade {

    UserProfileView update(Long userId, UpdateUserCommand command);
}
