package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityOverrideRepository;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityRuleRepository;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskCompletedEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskFailedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserDeletionListenerTest {

    private final AvailabilityOverrideRepository availabilityOverrideRepository = mock(AvailabilityOverrideRepository.class);
    private final AvailabilityRuleRepository availabilityRuleRepository = mock(AvailabilityRuleRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UserDeletionListener listener = new UserDeletionListener(
            availabilityOverrideRepository,
            availabilityRuleRepository,
            eventPublisher
    );

    @Test
    void deletesAvailabilityDataAndPublishesCompletedEvent() {
        listener.on(new UserDeletionRequestEvent(42L));

        verify(availabilityOverrideRepository).deleteByUserId(42L);
        verify(availabilityRuleRepository).deleteByUserId(42L);
        verify(eventPublisher).publishEvent(new UserDeletionTaskCompletedEvent(
                42L,
                UserDeletionModule.AVAILABILITY
        ));
    }

    @Test
    void publishesFailedEventAndRethrowsWhenDeletionFails() {
        doThrow(new RuntimeException("cleanup failed"))
                .when(availabilityOverrideRepository).deleteByUserId(42L);

        assertThatThrownBy(() -> listener.on(new UserDeletionRequestEvent(42L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cleanup failed");

        verify(eventPublisher).publishEvent(new UserDeletionTaskFailedEvent(
                42L,
                UserDeletionModule.AVAILABILITY,
                "cleanup failed"
        ));
        verifyNoInteractions(availabilityRuleRepository);
    }
}
