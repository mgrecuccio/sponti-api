package com.mgrtech.sponti_api.sms.internal.application;

import com.mgrtech.sponti_api.shared.error.*;
import com.mgrtech.sponti_api.sms.api.OtpFacade;
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
import com.mgrtech.sponti_api.user.api.UserVerificationFacade;
import com.mgrtech.sponti_api.user.api.command.VerifyUserPhoneCommand;
import com.mgrtech.sponti_api.user.api.event.UserCreatedEvent;
import com.mgrtech.sponti_api.user.api.event.UserPhoneNumberChangedEvent;
import com.mgrtech.sponti_api.user.api.query.UserContactInfoQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class SmsApplicationService implements OtpFacade {

    private static final Logger log = LoggerFactory.getLogger(SmsApplicationService.class);
    private static final int OTP_SENT = 10;
    private static final int OTP_VERIFIED = 11;
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(5);

    private final UserProfileQuery userProfileQuery;
    private final UserContactInfoQuery userContactInfoQuery;
    private final RestClient restClient;
    private final SmsProviderProperties smsProviderProperties;
    private final OtpRateLimitProperties otpRateLimitProperties;
    private final VerificationEntityRepository verificationEntityRepository;
    private final UserVerificationFacade userVerificationFacade;
    private final Clock clock;

    public SmsApplicationService(
            UserProfileQuery userProfileQuery,
            UserContactInfoQuery userContactInfoQuery,
            @Qualifier("smsBoxRestClient") RestClient restClient,
            SmsProviderProperties smsProviderProperties,
            OtpRateLimitProperties otpRateLimitProperties,
            VerificationEntityRepository verificationEntityRepository,
            UserVerificationFacade userVerificationFacade,
            Clock clock
    ) {
        this.userProfileQuery = userProfileQuery;
        this.userContactInfoQuery = userContactInfoQuery;
        this.restClient = restClient;
        this.smsProviderProperties = smsProviderProperties;
        this.otpRateLimitProperties = otpRateLimitProperties;
        this.verificationEntityRepository = verificationEntityRepository;
        this.userVerificationFacade = userVerificationFacade;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(UserPhoneNumberChangedEvent event) {
        sendOtpCode(new SendOtpCommand(event.userId(), event.phoneNumber()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(UserCreatedEvent event) {
        sendOtpCode(new SendOtpCommand(event.userId(), event.phoneNumber(), event.clientIp()));
    }

    @Override
    @Transactional
    public VerificationView resendPhoneVerification(Long userId) {
        return resendPhoneVerification(userId, null);
    }

    @Override
    @Transactional
    public VerificationView resendPhoneVerification(Long userId, String clientIp) {
        var phoneNumber = userContactInfoQuery.getPhoneNumber(userId)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        return sendOtpCode(new SendOtpCommand(userId, phoneNumber, clientIp));
    }

    @Override
    @Transactional
    public VerificationView sendOtpCode(SendOtpCommand command) {
        var userId = command.userId();
        var receivedPhoneNumber = command.phoneNumber();
        log.info("Sending OTP code to userId: {}, number: {}", userId, maskPhoneNumber(receivedPhoneNumber));

        var user = userProfileQuery.getProfileById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        assertPhoneNumberOwnership(userId, receivedPhoneNumber, user.phoneNumber());
        assertResendAllowed(command);

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
                VerificationPurpose.REGISTRATION,
                Instant.now(clock).plus(10, ChronoUnit.MINUTES)
        ));

        log.info("OTP code sent to userId: {}, number: {}", userId, maskPhoneNumber(receivedPhoneNumber));
        return new VerificationView(otpVerification.getId().toString());
    }

    @Override
    @Transactional
    public VerificationResultView verifyOtpCode(VerifyOtpCommand command) {
        var userId = command.userId();
        var verificationId = command.verificationId();

        log.info("Verifying OTP code for userId: {}, verificationId: {}", userId, verificationId);

        var user = userProfileQuery.getProfileById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        VerificationEntity verification = resolveVerification(command, user.phoneNumber());

        if (verification.isExpired()) {
            throw new ExpiredVerificationException();
        }

        if (verification.isVerified()) {
            throw new AlreadyVerifiedVerificationException();
        }

        if (verification.getAttempts() >= 3) {
            throw new TooManyAttemptsException();
        }

        assertPhoneNumberOwnership(userId, verification.getPhoneNumber(), user.phoneNumber());

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
        userVerificationFacade.verify(new VerifyUserPhoneCommand(userId));

        log.info("OTP code for userId: {}, verificationId: {} verified", userId, verificationId);
        return new VerificationResultView(userId, true);
    }

    private VerificationEntity resolveVerification(VerifyOtpCommand command, String phoneNumber) {
        if (command.verificationId() != null && !command.verificationId().isBlank()) {
            try {
                return verificationEntityRepository.findById(UUID.fromString(command.verificationId()))
                        .orElseGet(() -> latestPendingVerificationFor(phoneNumber));
            } catch (IllegalArgumentException ignored) {
                return latestPendingVerificationFor(phoneNumber);
            }
        }

        return latestPendingVerificationFor(phoneNumber);
    }

    private VerificationEntity latestPendingVerificationFor(String phoneNumber) {
        return verificationEntityRepository.findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
                        phoneNumber,
                        VerificationPurpose.REGISTRATION,
                        VerificationStatus.PENDING
                )
                .orElseThrow(VerificationNotFoundException::new);
    }

    private void assertPhoneNumberOwnership(Long userId, String receivedPhoneNumber, String userPhoneNumber) {
        if (!userPhoneNumber.equals(receivedPhoneNumber)) {
            throw new UnexpectedPhoneNumberException("Unexpected phone number for userId: " + userId +
                    ". Got unexpected " + maskPhoneNumber(receivedPhoneNumber));
        }
    }

    private void assertResendAllowed(SendOtpCommand command) {
        var cooldownStartedAt = Instant.now(clock).minus(RESEND_COOLDOWN);
        if (verificationEntityRepository.existsByPhoneNumberAndPurposeAndCreatedAtAfter(
                command.phoneNumber(),
                VerificationPurpose.REGISTRATION,
                cooldownStartedAt
        )) {
            throw new TooManyAttemptsException();
        }

        var rateLimitStartedAt = Instant.now(clock).minus(otpRateLimitProperties.window());
        if (verificationEntityRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                command.userId(),
                VerificationPurpose.REGISTRATION,
                rateLimitStartedAt
        ) >= otpRateLimitProperties.maxPerUser()) {
            throw new TooManyAttemptsException();
        }

        var clientIp = normalizedClientIp(command.clientIp());
        if (clientIp != null && verificationEntityRepository.countByClientIpAndPurposeAndCreatedAtAfter(
                clientIp,
                VerificationPurpose.REGISTRATION,
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

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "na";
        }

        var trimmed = phoneNumber.trim();
        if (trimmed.length() <= 4) {
            return "***";
        }

        return "***" + trimmed.substring(trimmed.length() - 4);
    }

    private String normalizedClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }

        return clientIp.trim();
    }
}
