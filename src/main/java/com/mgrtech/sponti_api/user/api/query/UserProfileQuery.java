package com.mgrtech.sponti_api.user.api.query;

import com.mgrtech.sponti_api.user.api.view.UserProfileView;

import java.util.Optional;

public interface UserProfileQuery {

    Optional<UserProfileView> getProfileById(Long userId);
}
