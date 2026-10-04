package com.mgrtech.sponti_api.contact.internal.application;

import com.mgrtech.sponti_api.contact.internal.repository.ContactInvitationRepository;
import com.mgrtech.sponti_api.contact.internal.repository.ContactRelationshipRepository;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskCompletedEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskFailedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserDeletionListenerTest {

    private final ContactInvitationRepository contactInvitationRepository = mock(ContactInvitationRepository.class);
    private final ContactRelationshipRepository contactRelationshipRepository = mock(ContactRelationshipRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UserDeletionListener listener = new UserDeletionListener(
            contactInvitationRepository,
            contactRelationshipRepository,
            eventPublisher
    );

    @Test
    void deletesContactDataAndPublishesCompletedEvent() {
        listener.on(new UserDeletionRequestEvent(42L));

        verify(contactInvitationRepository).deleteBySenderUserIdOrRecipientUserId(42L, 42L);
        verify(contactRelationshipRepository).deleteByOwnerUserIdOrContactUserId(42L, 42L);
        verify(eventPublisher).publishEvent(new UserDeletionTaskCompletedEvent(
                42L,
                UserDeletionModule.CONTACT
        ));
    }

    @Test
    void publishesFailedEventAndRethrowsWhenDeletionFails() {
        doThrow(new RuntimeException("cleanup failed"))
                .when(contactInvitationRepository).deleteBySenderUserIdOrRecipientUserId(42L, 42L);

        assertThatThrownBy(() -> listener.on(new UserDeletionRequestEvent(42L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cleanup failed");

        verify(eventPublisher).publishEvent(new UserDeletionTaskFailedEvent(
                42L,
                UserDeletionModule.CONTACT,
                "cleanup failed"
        ));
        verifyNoInteractions(contactRelationshipRepository);
    }
}
