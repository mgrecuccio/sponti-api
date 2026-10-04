package com.mgrtech.sponti_api.contact.internal.application;

import com.mgrtech.sponti_api.contact.internal.repository.ContactInvitationRepository;
import com.mgrtech.sponti_api.contact.internal.repository.ContactRelationshipRepository;
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

@Component("contactUserDeletionListener")
@AllArgsConstructor
public class UserDeletionListener {

    private final ContactInvitationRepository contactInvitationRepository;
    private final ContactRelationshipRepository contactRelationshipRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(UserDeletionRequestEvent event) {
        try {
            contactInvitationRepository.deleteBySenderUserIdOrRecipientUserId(event.userId(), event.userId());
            contactRelationshipRepository.deleteByOwnerUserIdOrContactUserId(event.userId(), event.userId());

            eventPublisher.publishEvent(new UserDeletionTaskCompletedEvent(
                    event.userId(),
                    UserDeletionModule.CONTACT
            ));
        } catch (RuntimeException ex) {
            eventPublisher.publishEvent(new UserDeletionTaskFailedEvent(
                    event.userId(),
                    UserDeletionModule.CONTACT,
                    ex.getMessage()
            ));
            throw ex;
        }
    }
}
