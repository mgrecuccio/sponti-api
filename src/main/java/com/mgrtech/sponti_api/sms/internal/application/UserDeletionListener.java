package com.mgrtech.sponti_api.sms.internal.application;

import com.mgrtech.sponti_api.sms.internal.repository.VerificationEntityRepository;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskCompletedEvent;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskFailedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("smsUserDeletionListener")
@AllArgsConstructor
@Slf4j
public class UserDeletionListener {

    private final ApplicationEventPublisher eventPublisher;
    private final VerificationEntityRepository verificationEntityRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(UserDeletionRequestEvent event) {
        try {
            verificationEntityRepository.deleteByUserId(event.userId());

            eventPublisher.publishEvent(new UserDeletionTaskCompletedEvent(
                    event.userId(),
                    UserDeletionModule.SMS
            ));
        } catch (RuntimeException ex) {
            eventPublisher.publishEvent(new UserDeletionTaskFailedEvent(
                    event.userId(),
                    UserDeletionModule.SMS,
                    ex.getMessage()
            ));
            throw ex;
        }
    }
}
