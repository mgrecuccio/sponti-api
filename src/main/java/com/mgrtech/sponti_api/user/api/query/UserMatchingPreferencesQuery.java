package com.mgrtech.sponti_api.user.api.query;

import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;

import java.util.List;
import java.util.Optional;

public interface UserMatchingPreferencesQuery {

    Optional<UserMatchingPreferencesView> getMatchingPreferences(Long userId);

    List<Long> getMatchingEnabledUserIds();
}
