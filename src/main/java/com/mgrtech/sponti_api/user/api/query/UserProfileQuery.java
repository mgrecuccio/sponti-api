package com.mgrtech.sponti_api.user.api.query;

import com.mgrtech.sponti_api.user.api.view.UserProfileView;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface UserProfileQuery {

    Optional<UserProfileView> getProfileById(Long userId);

    Map<Long, UserProfileView> getProfilesByIds(Collection<Long> userIds);
}
