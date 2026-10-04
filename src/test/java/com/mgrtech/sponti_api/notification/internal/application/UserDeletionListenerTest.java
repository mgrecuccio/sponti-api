package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.internal.repository.NotificationDeviceTokenRepository;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationHistoryRepository;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskCompletedEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskFailedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserDeletionListenerTest {

    private final NotificationHistoryRepository notificationHistoryRepository = mock(NotificationHistoryRepository.class);
    private final NotificationDeviceTokenRepository notificationDeviceTokenRepository = mock(NotificationDeviceTokenRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UserDeletionListener listener = new UserDeletionListener(
            notificationHistoryRepository,
            notificationDeviceTokenRepository,
            eventPublisher
    );

    @Test
    void deletesNotificationDataAndPublishesCompletedEvent() {
        listener.on(new UserDeletionRequestEvent(42L));

        verify(notificationHistoryRepository).deleteByUserId(42L);
        verify(notificationDeviceTokenRepository).deleteByUserId(42L);
        verify(eventPublisher).publishEvent(new UserDeletionTaskCompletedEvent(
                42L,
                UserDeletionModule.NOTIFICATION
        ));
    }

    @Test
    void publishesFailedEventAndRethrowsWhenDeletionFails() {
        doThrow(new RuntimeException("cleanup failed"))
                .when(notificationHistoryRepository).deleteByUserId(42L);

        assertThatThrownBy(() -> listener.on(new UserDeletionRequestEvent(42L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cleanup failed");

        verify(eventPublisher).publishEvent(new UserDeletionTaskFailedEvent(
                42L,
                UserDeletionModule.NOTIFICATION,
                "cleanup failed"
        ));
        verifyNoInteractions(notificationDeviceTokenRepository);
    }
}
