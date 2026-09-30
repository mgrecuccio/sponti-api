package com.mgrtech.sponti_api.sms.internal.application;

import com.mgrtech.sponti_api.shared.error.TooManyAttemptsException;
import com.mgrtech.sponti_api.shared.error.VerificationNotFoundException;
import com.mgrtech.sponti_api.shared.error.ExpiredVerificationException;
import com.mgrtech.sponti_api.sms.api.OtpPurpose;
import com.mgrtech.sponti_api.sms.api.command.SendOtpCommand;
import com.mgrtech.sponti_api.sms.api.command.VerifyOtpCommand;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationEntity;
import com.mgrtech.sponti_api.sms.internal.configuration.OtpRateLimitProperties;
import com.mgrtech.sponti_api.sms.internal.configuration.SmsProviderProperties;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationPurpose;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationStatus;
import com.mgrtech.sponti_api.sms.internal.repository.VerificationEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-26T08:00:00Z");

    private final RestClient restClient = mock(RestClient.class);
    private final VerificationEntityRepository verificationEntityRepository = mock(VerificationEntityRepository.class);
    private final SmsApplicationService service = new SmsApplicationService(
            restClient,
            new SmsProviderProperties("https://sms.example", "api-key", "Your code is {OTP}"),
            new OtpRateLimitProperties(Duration.ofHours(1), 5, 20),
            verificationEntityRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void sendOtpCodeRejectsWhenRegistrationOtpWasSentWithinCooldown() {
        when(verificationEntityRepository.existsByPhoneNumberAndPurposeAndCreatedAtAfter(
                eq("+32468009911"),
                eq(VerificationPurpose.REGISTRATION),
                any(Instant.class)
        )).thenReturn(true);

        assertThatThrownBy(() -> service.sendOtpCode(
                new SendOtpCommand(42L, "+32468009911"),
                OtpPurpose.REGISTRATION
        ))
                .isInstanceOf(TooManyAttemptsException.class);

        verify(verificationEntityRepository).existsByPhoneNumberAndPurposeAndCreatedAtAfter(
                "+32468009911",
                VerificationPurpose.REGISTRATION,
                NOW.minusSeconds(300)
        );
        verifyNoInteractions(restClient);
    }

    @Test
    void sendOtpCodeRejectsWhenUserReachedHourlyLimit() {
        when(verificationEntityRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                eq(42L),
                eq(VerificationPurpose.REGISTRATION),
                any(Instant.class)
        )).thenReturn(5L);

        assertThatThrownBy(() -> service.sendOtpCode(
                new SendOtpCommand(42L, "+32468009911", "203.0.113.10"),
                OtpPurpose.REGISTRATION
        ))
                .isInstanceOf(TooManyAttemptsException.class);

        verify(verificationEntityRepository).countByUserIdAndPurposeAndCreatedAtAfter(
                42L,
                VerificationPurpose.REGISTRATION,
                NOW.minusSeconds(3600)
        );
        verifyNoInteractions(restClient);
    }

    @Test
    void sendOtpCodeRejectsWhenClientIpReachedHourlyLimit() {
        when(verificationEntityRepository.countByClientIpAndPurposeAndCreatedAtAfter(
                eq("203.0.113.10"),
                eq(VerificationPurpose.REGISTRATION),
                any(Instant.class)
        )).thenReturn(20L);

        assertThatThrownBy(() -> service.sendOtpCode(
                new SendOtpCommand(42L, "+32468009911", "203.0.113.10"),
                OtpPurpose.REGISTRATION
        ))
                .isInstanceOf(TooManyAttemptsException.class);

        verify(verificationEntityRepository).countByClientIpAndPurposeAndCreatedAtAfter(
                "203.0.113.10",
                VerificationPurpose.REGISTRATION,
                NOW.minusSeconds(3600)
        );
        verifyNoInteractions(restClient);
    }

    @Test
    void verifyOtpCodeWithoutVerificationIdUsesLatestPendingVerificationForUserPhoneNumber() {
        when(verificationEntityRepository.findFirstByPhoneNumberAndUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                "+32468009911",
                42L,
                VerificationPurpose.REGISTRATION,
                VerificationStatus.PENDING
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyOtpCode(new VerifyOtpCommand(
                42L,
                "+32468009911",
                null,
                "123456",
                OtpPurpose.REGISTRATION
        )))
                .isInstanceOf(VerificationNotFoundException.class);

        verify(verificationEntityRepository).findFirstByPhoneNumberAndUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                "+32468009911",
                42L,
                VerificationPurpose.REGISTRATION,
                VerificationStatus.PENDING
        );
        verifyNoInteractions(restClient);
    }

    @Test
    void verifyOtpCodeWithUnknownVerificationIdFallsBackToLatestPendingVerificationForUserPhoneNumber() {
        var verificationId = UUID.randomUUID();
        var expiredVerification = expiredVerification();

        when(verificationEntityRepository.findById(verificationId)).thenReturn(Optional.empty());
        when(verificationEntityRepository.findFirstByPhoneNumberAndUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                "+32468009911",
                42L,
                VerificationPurpose.REGISTRATION,
                VerificationStatus.PENDING
        )).thenReturn(Optional.of(expiredVerification));

        assertThatThrownBy(() -> service.verifyOtpCode(new VerifyOtpCommand(
                42L,
                "+32468009911",
                verificationId.toString(),
                "123456",
                OtpPurpose.REGISTRATION
        )))
                .isInstanceOf(ExpiredVerificationException.class);

        verify(verificationEntityRepository).findById(verificationId);
        verify(verificationEntityRepository).findFirstByPhoneNumberAndUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                "+32468009911",
                42L,
                VerificationPurpose.REGISTRATION,
                VerificationStatus.PENDING
        );
        verifyNoInteractions(restClient);
    }

    @Test
    void verifyOtpCodeWithNonBackendVerificationIdFallsBackToLatestPendingVerificationForUserPhoneNumber() {
        var expiredVerification = expiredVerification();

        when(verificationEntityRepository.findFirstByPhoneNumberAndUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                "+32468009911",
                42L,
                VerificationPurpose.REGISTRATION,
                VerificationStatus.PENDING
        )).thenReturn(Optional.of(expiredVerification));

        assertThatThrownBy(() -> service.verifyOtpCode(new VerifyOtpCommand(
                42L,
                "+32468009911",
                "pending-verification-id",
                "123456",
                OtpPurpose.REGISTRATION
        )))
                .isInstanceOf(ExpiredVerificationException.class);

        verify(verificationEntityRepository).findFirstByPhoneNumberAndUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                "+32468009911",
                42L,
                VerificationPurpose.REGISTRATION,
                VerificationStatus.PENDING
        );
        verifyNoInteractions(restClient);
    }

    private VerificationEntity expiredVerification() {
        return new VerificationEntity(
                "+32468009911",
                42L,
                null,
                VerificationPurpose.REGISTRATION,
                Instant.EPOCH
        );
    }
}
