package com.mgrtech.sponti_api.sms.internal.application;

import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskCompletedEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskFailedEvent;
import com.mgrtech.sponti_api.sms.internal.repository.VerificationEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserDeletionListenerTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final VerificationEntityRepository verificationEntityRepository = mock(VerificationEntityRepository.class);
    private final UserDeletionListener listener = new UserDeletionListener(
            eventPublisher,
            verificationEntityRepository
    );

    @Test
    void deletesSmsDataAndPublishesCompletedEvent() {
        listener.on(new UserDeletionRequestEvent(42L));

        verify(verificationEntityRepository).deleteByUserId(42L);
        verify(eventPublisher).publishEvent(new UserDeletionTaskCompletedEvent(
                42L,
                UserDeletionModule.SMS
        ));
    }

    @Test
    void publishesFailedEventAndRethrowsWhenDeletionFails() {
        doThrow(new RuntimeException("cleanup failed"))
                .when(verificationEntityRepository).deleteByUserId(42L);

        assertThatThrownBy(() -> listener.on(new UserDeletionRequestEvent(42L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cleanup failed");

        verify(eventPublisher).publishEvent(new UserDeletionTaskFailedEvent(
                42L,
                UserDeletionModule.SMS,
                "cleanup failed"
        ));
    }
}
