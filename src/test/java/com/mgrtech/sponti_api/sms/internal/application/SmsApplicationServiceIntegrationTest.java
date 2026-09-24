package com.mgrtech.sponti_api.sms.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.FullIntegrationTest;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationPurpose;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationStatus;
import com.mgrtech.sponti_api.sms.internal.repository.VerificationEntityRepository;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.internal.application.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@FullIntegrationTest
class SmsApplicationServiceIntegrationTest {

    @Autowired
    UserRegistrationFacade userRegistrationFacade;

    @Autowired
    UserFacade userFacade;

    @Autowired
    VerificationEntityRepository verificationEntityRepository;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    void phoneNumberChangedEventPersistsOtpVerification() {
        var created = userRegistrationFacade.createUser(new CreateUserCommand(
                "password-hash",
                "John",
                "+32468009911",
                "Europe/Brussels"
        ));

        userFacade.updateProfile(created.id(), new UpdateUserCommand(
                "John",
                "Europe/Brussels",
                "+32468009912"
        ));

        var verification = verificationEntityRepository
                .findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                        "+32468009912",
                        VerificationPurpose.REGISTRATION,
                        VerificationStatus.PENDING
                );

        assertThat(verification).isPresent();
        assertThat(verification.get().getUserId()).isEqualTo(created.id());
    }
}
