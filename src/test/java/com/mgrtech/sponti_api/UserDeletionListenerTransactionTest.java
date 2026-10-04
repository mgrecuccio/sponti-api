package com.mgrtech.sponti_api;

import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class UserDeletionListenerTransactionTest {

    private static final Class<?>[] USER_DELETION_LISTENERS = {
            com.mgrtech.sponti_api.availability.internal.application.UserDeletionListener.class,
            com.mgrtech.sponti_api.contact.internal.application.UserDeletionListener.class,
            com.mgrtech.sponti_api.matching.internal.application.UserDeletionListener.class,
            com.mgrtech.sponti_api.notification.internal.application.UserDeletionListener.class,
            com.mgrtech.sponti_api.sms.internal.application.UserDeletionListener.class
    };

    @Test
    void deletionRequestListenersRunAfterCommitInANewTransaction() throws NoSuchMethodException {
        for (Class<?> listener : USER_DELETION_LISTENERS) {
            Method method = listener.getDeclaredMethod("on", UserDeletionRequestEvent.class);

            assertThat(method.getAnnotation(TransactionalEventListener.class).phase())
                    .as("%s event phase", listener.getName())
                    .isEqualTo(TransactionPhase.AFTER_COMMIT);
            assertThat(method.getAnnotation(Transactional.class).propagation())
                    .as("%s transaction propagation", listener.getName())
                    .isEqualTo(Propagation.REQUIRES_NEW);
        }
    }
}
