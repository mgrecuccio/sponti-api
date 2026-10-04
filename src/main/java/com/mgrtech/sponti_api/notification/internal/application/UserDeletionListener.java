package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.internal.repository.NotificationDeviceTokenRepository;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationHistoryRepository;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskCompletedEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskFailedEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("notificationUserDeletionListener")
@AllArgsConstructor
public class UserDeletionListener {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationDeviceTokenRepository notificationDeviceTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(UserDeletionRequestEvent event) {
        try {
            notificationHistoryRepository.deleteByUserId(event.userId());
            notificationDeviceTokenRepository.deleteByUserId(event.userId());

            eventPublisher.publishEvent(new UserDeletionTaskCompletedEvent(
                    event.userId(),
                    UserDeletionModule.NOTIFICATION
            ));
        } catch (RuntimeException ex) {
            eventPublisher.publishEvent(new UserDeletionTaskFailedEvent(
                    event.userId(),
                    UserDeletionModule.NOTIFICATION,
                    ex.getMessage()
            ));
            throw ex;
        }
    }
}
