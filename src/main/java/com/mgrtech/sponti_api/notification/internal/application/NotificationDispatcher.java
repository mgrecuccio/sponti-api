package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import com.mgrtech.sponti_api.notification.internal.delivery.PushDeliveryResult;
import com.mgrtech.sponti_api.notification.internal.delivery.PushFailureType;
import com.mgrtech.sponti_api.notification.internal.delivery.PushMessage;
import com.mgrtech.sponti_api.notification.internal.delivery.PushNotificationSender;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeviceTokenEntity;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationProvider;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationDeviceTokenRepository;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final Clock clock;
    private final NotificationProperties properties;
    private final NotificationDeviceTokenRepository tokenRepository;
    private final PushNotificationSender sender;
    private final OperationalMetrics metrics;

    @Transactional
    public void dispatch(NotificationHistoryEntity history, SendNotificationCommand command) {
        var now = Instant.now(clock);

        if (!properties.pushEnabled()) {
            history.markSent(NotificationProvider.FCM, "push-disabled:" + now, now);
            log.info("Notification marked sent without push: userId={} type={} reason=push-disabled", command.userId(), command.type());
            return;
        }

        var tokens = tokenRepository.findByUserIdAndEnabledTrue(command.userId());

        if (tokens.isEmpty()) {
            history.markNoDeviceToken(now);
            metrics.notificationDeliveryFailure(command.type().name(), "no_device_token");
            log.info("Notification delivery skipped: userId={} type={} reason=no-device-token", command.userId(), command.type());
            return;
        }

        var results = tokens.stream()
                .map(token -> deliver(token, command))
                .toList();

        results.stream()
                .filter(PushDeliveryResult::success)
                .findFirst()
                .ifPresentOrElse(
                        result -> history.markSent(NotificationProvider.FCM, result.providerMessageId(), Instant.now(clock)),
                        () -> markFailure(history, results, Instant.now(clock))
                );
    }

    private PushDeliveryResult deliver(NotificationDeviceTokenEntity token, SendNotificationCommand command) {
        var result = sender.send(new PushMessage(
                token.getToken(),
                command.type(),
                command.title(),
                command.body(),
                command.data()
        ));

        if (!result.success() && result.failureType() == PushFailureType.PERMANENT) {
            token.disable(Instant.now(clock));
            log.warn("Notification device token disabled: userId={} reason=permanent-delivery-failure", token.getUserId());
        }

        return result;
    }

    private void markFailure(NotificationHistoryEntity history, List<PushDeliveryResult> results, Instant now) {
        var primary = results.stream()
                .filter(result -> result.failureType() == PushFailureType.TRANSIENT)
                .findFirst()
                .orElseGet(() -> results.isEmpty()
                        ? PushDeliveryResult.failed("UNKNOWN", "No push delivery result was produced.", PushFailureType.CONFIGURATION)
                        : results.getFirst());

        if (primary.failureType() == PushFailureType.TRANSIENT && history.getAttemptCount() < properties.retry().maxAttempts()) {
            history.markRetryPending(
                    primary.failureCode(),
                    primary.failureReason(),
                    now,
                    now.plus(retryDelay(history.getAttemptCount()))
            );
            metrics.notificationDeliveryFailure(history.getType().name(), primary.failureType().name());
            log.warn(
                    "Notification delivery failed transiently: notificationHistoryId={} type={} failureCode={} nextRetryAt={}",
                    history.getId(),
                    history.getType(),
                    primary.failureCode(),
                    history.getNextRetryAt()
            );
            return;
        }

        history.markFailed(primary.failureCode(), primary.failureReason(), now);
        metrics.notificationDeliveryFailure(history.getType().name(), primary.failureType().name());
        log.warn(
                "Notification delivery failed permanently: notificationHistoryId={} type={} failureCode={} failureType={}",
                history.getId(),
                history.getType(),
                primary.failureCode(),
                primary.failureType()
        );
    }

    private java.time.Duration retryDelay(int attemptCount) {
        return properties.retry().fixedDelay().multipliedBy(Math.max(1, attemptCount + 1L));
    }
}
