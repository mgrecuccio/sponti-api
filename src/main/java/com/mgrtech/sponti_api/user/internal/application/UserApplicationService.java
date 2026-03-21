package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.user.api.UserFacade;
import com.mgrtech.sponti_api.user.api.dto.UserProfileView;
import org.springframework.stereotype.Service;

@Service
public class UserApplicationService implements UserFacade {

    @Override
    public UserProfileView getProfile(Long userId) {
        return new UserProfileView(1L, "todo@sponti.app", "TODO", "ACTIVE");
    }
}
