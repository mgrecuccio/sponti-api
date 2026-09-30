package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.sms.api.OtpFacade;
import com.mgrtech.sponti_api.sms.api.OtpPurpose;
import com.mgrtech.sponti_api.sms.api.command.SendOtpCommand;
import com.mgrtech.sponti_api.user.api.event.UserCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
class RegistrationOtpListener {

    private final OtpFacade otpFacade;

    RegistrationOtpListener(OtpFacade otpFacade) {
        this.otpFacade = otpFacade;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(UserCreatedEvent event) {
        otpFacade.sendOtpCode(
                new SendOtpCommand(event.userId(), event.phoneNumber(), event.clientIp()),
                OtpPurpose.REGISTRATION
        );
    }
}
