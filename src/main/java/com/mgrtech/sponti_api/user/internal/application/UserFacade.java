package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.view.UserPrivateProfileView;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;

public interface UserFacade {

    UserProfileView updateProfile(Long userId, UpdateUserCommand command);

    UserPrivateProfileView getCurrentUserProfile(Long userId);
}
