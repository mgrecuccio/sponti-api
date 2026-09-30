package com.mgrtech.sponti_api.sms.internal.application;

import com.mgrtech.sponti_api.shared.error.*;
import com.mgrtech.sponti_api.sms.api.OtpFacade;
import com.mgrtech.sponti_api.sms.api.OtpPurpose;
import com.mgrtech.sponti_api.sms.api.command.SendOtpCommand;
import com.mgrtech.sponti_api.sms.api.command.VerifyOtpCommand;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;
import com.mgrtech.sponti_api.sms.internal.application.request.SmsBoxOtpRequest;
import com.mgrtech.sponti_api.sms.internal.application.response.SmsBoxOtpSendResponse;
import com.mgrtech.sponti_api.sms.internal.application.response.SmsBoxOtpVerifyResponse;
import com.mgrtech.sponti_api.sms.internal.configuration.OtpRateLimitProperties;
import com.mgrtech.sponti_api.sms.internal.configuration.SmsProviderProperties;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationEntity;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationPurpose;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationStatus;
import com.mgrtech.sponti_api.sms.internal.repository.VerificationEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.maskPhoneNumber;
import static com.mgrtech.sponti_api.shared.utils.StringUtils.normalizedClientIp;

@Service
public class SmsApplicationService implements OtpFacade {

    private static final Logger log = LoggerFactory.getLogger(SmsApplicationService.class);
    private static final int OTP_SENT = 10;
    private static final int OTP_VERIFIED = 11;
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final SmsProviderProperties smsProviderProperties;
    private final OtpRateLimitProperties otpRateLimitProperties;
    private final VerificationEntityRepository verificationEntityRepository;
    private final Clock clock;

    public SmsApplicationService(
            @Qualifier("smsBoxRestClient") RestClient restClient,
            SmsProviderProperties smsProviderProperties,
            OtpRateLimitProperties otpRateLimitProperties,
            VerificationEntityRepository verificationEntityRepository,
            Clock clock
    ) {
        this.restClient = restClient;
        this.smsProviderProperties = smsProviderProperties;
        this.otpRateLimitProperties = otpRateLimitProperties;
        this.verificationEntityRepository = verificationEntityRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public VerificationView sendOtpCode(
            SendOtpCommand command,
            OtpPurpose purpose
    ) {
        var verificationPurpose = toVerificationPurpose(purpose);
        var userId = command.userId();
        var receivedPhoneNumber = command.phoneNumber();
        log.info("Sending OTP code to userId: {}, number: {}, purpose={}", userId, maskPhoneNumber(receivedPhoneNumber), verificationPurpose);

        assertResendAllowed(command, verificationPurpose);

        var request = new SmsBoxOtpRequest(
                toSmsBoxNumber(receivedPhoneNumber),
                smsProviderProperties.otpText()
        );

        var response = restClient.post()
                .uri("/v2/otp/send")
                .body(request)
                .retrieve()
                .body(SmsBoxOtpSendResponse.class);

        if (response == null || response.code() != OTP_SENT) {
            throw new PhoneVerificationException(
                    "Unable to send verification code"
            );
        }

        var otpVerification = verificationEntityRepository.save(new VerificationEntity(
                receivedPhoneNumber,
                userId,
                normalizedClientIp(command.clientIp()),
                verificationPurpose,
                Instant.now(clock).plus(10, ChronoUnit.MINUTES)
        ));

        log.info("OTP code sent to userId: {}, number: {}, purpose={}", userId, maskPhoneNumber(receivedPhoneNumber), verificationPurpose);
        return new VerificationView(otpVerification.getId().toString());
    }

    @Override
    @Transactional
    public VerificationResultView verifyOtpCode(VerifyOtpCommand command) {
        var userId = command.userId();
        var verificationId = command.verificationId();
        var purpose = toVerificationPurpose(command.purpose());

        log.info("Verifying OTP code for userId: {}, verificationId: {}, purpose={}", userId, verificationId, purpose);

        VerificationEntity verification = resolveVerification(command, command.phoneNumber(), purpose);

        if (verification.isExpired()) {
            throw new ExpiredVerificationException();
        }

        if (verification.isVerified()) {
            throw new AlreadyVerifiedVerificationException();
        }

        if (verification.getAttempts() >= 3) {
            throw new TooManyAttemptsException();
        }

        assertPhoneNumberOwnership(userId, verification.getPhoneNumber(), command.phoneNumber());

        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/otp/verify")
                        .queryParam("otp", command.otpCode())
                        .build())
                .retrieve()
                .body(SmsBoxOtpVerifyResponse.class);

        if (response == null || response.code() != OTP_VERIFIED) {
            verification.recordFailedAttempt();
            throw new InvalidVerificationCodeException();
        }

        verification.markVerified();

        log.info("OTP code for userId: {}, verificationId: {}, purpose={} verified", userId, verificationId, purpose);
        return new VerificationResultView(userId, true);
    }

    private VerificationEntity resolveVerification(
            VerifyOtpCommand command,
            String phoneNumber,
            VerificationPurpose purpose
    ) {
        if (command.verificationId() != null && !command.verificationId().isBlank()) {
            try {
                return verificationEntityRepository.findById(UUID.fromString(command.verificationId()))
                        .filter(verification -> verification.getUserId().equals(command.userId()))
                        .filter(verification -> verification.getPhoneNumber().equals(phoneNumber))
                        .filter(verification -> verification.getPurpose() == purpose)
                        .orElseGet(() -> latestPendingVerificationFor(phoneNumber, command.userId(), purpose));
            } catch (IllegalArgumentException ignored) {
                return latestPendingVerificationFor(phoneNumber, command.userId(), purpose);
            }
        }

        return latestPendingVerificationFor(phoneNumber, command.userId(), purpose);
    }

    private VerificationEntity latestPendingVerificationFor(String phoneNumber, Long userId, VerificationPurpose purpose) {
        return verificationEntityRepository.findFirstByPhoneNumberAndUserIdAndPurposeAndStatusOrderByCreatedAtDesc(
                        phoneNumber,
                        userId,
                        purpose,
                        VerificationStatus.PENDING
                )
                .orElseThrow(VerificationNotFoundException::new);
    }

    private VerificationPurpose toVerificationPurpose(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTRATION -> VerificationPurpose.REGISTRATION;
            case PROFILE_UPDATE -> VerificationPurpose.PROFILE_UPDATE;
            case PASSWORD_RECOVERY -> VerificationPurpose.PASSWORD_RECOVERY;
        };
    }

    private void assertPhoneNumberOwnership(Long userId, String receivedPhoneNumber, String userPhoneNumber) {
        if (!userPhoneNumber.equals(receivedPhoneNumber)) {
            throw new UnexpectedPhoneNumberException("Unexpected phone number for userId: " + userId +
                    ". Got unexpected " + maskPhoneNumber(receivedPhoneNumber));
        }
    }

    private void assertResendAllowed(SendOtpCommand command, VerificationPurpose purpose) {
        var cooldownStartedAt = Instant.now(clock).minus(RESEND_COOLDOWN);
        if (verificationEntityRepository.existsByPhoneNumberAndPurposeAndCreatedAtAfter(
                command.phoneNumber(),
                purpose,
                cooldownStartedAt
        )) {
            throw new TooManyAttemptsException();
        }

        var rateLimitStartedAt = Instant.now(clock).minus(otpRateLimitProperties.window());
        if (verificationEntityRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                command.userId(),
                purpose,
                rateLimitStartedAt
        ) >= otpRateLimitProperties.maxPerUser()) {
            throw new TooManyAttemptsException();
        }

        var clientIp = normalizedClientIp(command.clientIp());
        if (clientIp != null && verificationEntityRepository.countByClientIpAndPurposeAndCreatedAtAfter(
                clientIp,
                purpose,
                rateLimitStartedAt
        ) >= otpRateLimitProperties.maxPerIp()) {
            throw new TooManyAttemptsException();
        }
    }

    private String toSmsBoxNumber(String phoneNumber) {
        return phoneNumber.startsWith("+")
                ? phoneNumber.substring(1)
                : phoneNumber;
    }
}
