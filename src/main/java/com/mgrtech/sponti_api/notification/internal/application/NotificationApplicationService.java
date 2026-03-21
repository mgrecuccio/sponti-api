package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.NotificationFacade;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class NotificationApplicationService implements NotificationFacade {

    private static final Logger log = LoggerFactory.getLogger(NotificationApplicationService.class);

    @Override
    public void send(SendNotificationCommand command) {
        log.info("Placeholder notification for user {}: {}", command.userId(), command.title());
    }
}
