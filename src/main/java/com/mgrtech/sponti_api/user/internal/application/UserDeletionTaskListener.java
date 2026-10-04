package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskCompletedEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskFailedEvent;
import com.mgrtech.sponti_api.user.api.deletion.UserDeletionFacade;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserDeletionTaskListener {

    private final UserDeletionFacade userDeletionFacade;

    public UserDeletionTaskListener(UserDeletionFacade userDeletionFacade) {
        this.userDeletionFacade = userDeletionFacade;
    }

    @EventListener
    void on(UserDeletionTaskCompletedEvent event) {
        userDeletionFacade.markTaskCompleted(event.userId(), event.module());
    }

    @EventListener
    void on(UserDeletionTaskFailedEvent event) {
        userDeletionFacade.markTaskFailed(
                event.userId(),
                event.module(),
                event.reason()
        );
    }
}
