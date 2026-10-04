package com.mgrtech.sponti_api.user.api.deletion;

import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;

public interface UserDeletionFacade {

    void requestDeletion(Long userId);

    void markTaskCompleted(Long userId, UserDeletionModule module);

    void markTaskFailed(Long userId, UserDeletionModule module, String reason);
}
