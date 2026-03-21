package com.mgrtech.sponti_api.user.api;

import com.mgrtech.sponti_api.user.api.dto.UserProfileView;

public interface UserFacade {

    UserProfileView getProfile(Long userId);
}
