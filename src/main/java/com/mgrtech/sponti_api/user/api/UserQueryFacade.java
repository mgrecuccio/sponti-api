package com.mgrtech.sponti_api.user.api;

import java.util.Optional;

public interface UserQueryFacade {

    Optional<UserProfileView> getProfileById(Long userId);
}
