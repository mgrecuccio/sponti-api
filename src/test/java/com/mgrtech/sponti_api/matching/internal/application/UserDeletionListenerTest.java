package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.internal.repository.MatchProposalRepository;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskCompletedEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskFailedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserDeletionListenerTest {

    private final MatchProposalRepository matchProposalRepository = mock(MatchProposalRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UserDeletionListener listener = new UserDeletionListener(
            matchProposalRepository,
            eventPublisher
    );

    @Test
    void deletesMatchingDataAndPublishesCompletedEvent() {
        listener.on(new UserDeletionRequestEvent(42L));

        verify(matchProposalRepository).deleteByInitiatorUserIdOrCandidateUserId(42L, 42L);
        verify(eventPublisher).publishEvent(new UserDeletionTaskCompletedEvent(
                42L,
                UserDeletionModule.MATCHING
        ));
    }

    @Test
    void publishesFailedEventAndRethrowsWhenDeletionFails() {
        doThrow(new RuntimeException("cleanup failed"))
                .when(matchProposalRepository).deleteByInitiatorUserIdOrCandidateUserId(42L, 42L);

        assertThatThrownBy(() -> listener.on(new UserDeletionRequestEvent(42L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cleanup failed");

        verify(eventPublisher).publishEvent(new UserDeletionTaskFailedEvent(
                42L,
                UserDeletionModule.MATCHING,
                "cleanup failed"
        ));
    }
}
