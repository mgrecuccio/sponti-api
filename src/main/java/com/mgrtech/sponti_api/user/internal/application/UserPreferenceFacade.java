package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.user.api.command.UpdatePreferencesCommand;
import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;

public interface UserPreferenceFacade {

    UserMatchingPreferencesView updatePreferences(Long userId, UpdatePreferencesCommand command);

}
