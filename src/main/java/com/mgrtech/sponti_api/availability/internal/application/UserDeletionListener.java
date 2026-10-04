package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityOverrideRepository;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityRuleRepository;
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

@Component("availabilityUserDeletionListener")
@AllArgsConstructor
public class UserDeletionListener {

    private final AvailabilityOverrideRepository availabilityOverrideRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(UserDeletionRequestEvent event) {
        try {
            availabilityOverrideRepository.deleteByUserId(event.userId());
            availabilityRuleRepository.deleteByUserId(event.userId());

            eventPublisher.publishEvent(new UserDeletionTaskCompletedEvent(
                    event.userId(),
                    UserDeletionModule.AVAILABILITY
            ));
        } catch (RuntimeException ex) {
            eventPublisher.publishEvent(new UserDeletionTaskFailedEvent(
                    event.userId(),
                    UserDeletionModule.AVAILABILITY,
                    ex.getMessage()
            ));
            throw ex;
        }
    }
}
