package com.mgrtech.sponti_api.notification.api;

import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;

public interface NotificationFacade {

    void send(SendNotificationCommand command);
}
