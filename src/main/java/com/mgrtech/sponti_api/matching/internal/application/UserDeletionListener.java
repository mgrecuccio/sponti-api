package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.internal.repository.MatchProposalRepository;
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

@Component("matchingUserDeletionListener")
@AllArgsConstructor
public class UserDeletionListener {

    private final MatchProposalRepository matchProposalRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(UserDeletionRequestEvent event) {
        try {
            matchProposalRepository.deleteByInitiatorUserIdOrCandidateUserId(event.userId(), event.userId());

            eventPublisher.publishEvent(new UserDeletionTaskCompletedEvent(
                    event.userId(),
                    UserDeletionModule.MATCHING
            ));
        } catch (RuntimeException ex) {
            eventPublisher.publishEvent(new UserDeletionTaskFailedEvent(
                    event.userId(),
                    UserDeletionModule.MATCHING,
                    ex.getMessage()
            ));
            throw ex;
        }
    }
}
