package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeliveryStatus;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationHistoryRepository;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
class NotificationRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryScheduler.class);

    private final Clock clock;
    private final NotificationProperties properties;
    private final NotificationHistoryRepository repository;
    private final NotificationDispatcher dispatcher;
    private final OperationalMetrics metrics;

    @Scheduled(
            fixedDelayString = "${sponti.notification.retry.fixed-delay}",
            initialDelayString = "${sponti.notification.retry.fixed-delay}"
    )
    @Transactional
    void retryDueNotifications() {
        var startedAt = Instant.now(clock);
        if (!properties.retry().enabled()) {
            metrics.schedulerDuration("notification_retry", "skipped", Duration.between(startedAt, Instant.now(clock)));
            log.debug("Notification retry scheduler skipped: reason=disabled");
            return;
        }

        try {
            var now = Instant.now(clock);
            var histories = repository.findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                    NotificationDeliveryStatus.RETRY_PENDING,
                    now
            );

            log.info("Notification retry scheduler found due notifications: count={}", histories.size());
            for (var history : histories) {
                if (history.getAttemptCount() >= properties.retry().maxAttempts()) {
                    history.markFailed(history.getFailureCode(), history.getFailureReason(), now);
                    metrics.notificationRetryVolume("max_attempts_exceeded");
                    log.warn("Notification retry skipped: notificationHistoryId={} reason=max-attempts-exceeded", history.getId());
                    continue;
                }

                metrics.notificationRetryVolume("dispatched");
                log.info("Retrying notification delivery: notificationHistoryId={}", history.getId());
                dispatcher.dispatch(history, commandFrom(history));
            }
            metrics.schedulerDuration("notification_retry", "success", Duration.between(startedAt, Instant.now(clock)));
        } catch (RuntimeException ex) {
            metrics.schedulerDuration("notification_retry", "failure", Duration.between(startedAt, Instant.now(clock)));
            log.error("Notification retry scheduler failed", ex);
            throw ex;
        }
    }

    private SendNotificationCommand commandFrom(NotificationHistoryEntity history) {
        var metadata = metadataMap(history.getMetadata());
        return new SendNotificationCommand(
                history.getUserId(),
                history.getType(),
                metadata.getOrDefault("title", titleFor(history.getType())),
                metadata.getOrDefault("body", bodyFor(history.getType())),
                dataOnly(metadata)
        );
    }

    private Map<String, String> metadataMap(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }

        return Arrays.stream(metadata.split(";"))
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (left, right) -> right));
    }

    private Map<String, String> dataOnly(Map<String, String> metadata) {
        return metadata.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals("title"))
                .filter(entry -> !entry.getKey().equals("body"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String titleFor(NotificationType type) {
        return switch (type) {
            case MATCH_PROPOSAL_CREATED -> "New match invitation";
            case MATCH_PROPOSAL_ACCEPTED -> "It's a match";
            case MATCH_SUGGESTIONS_AVAILABLE -> "New suggestions available";
        };
    }

    private String bodyFor(NotificationType type) {
        return switch (type) {
            case MATCH_PROPOSAL_CREATED -> "Someone wants to connect now.";
            case MATCH_PROPOSAL_ACCEPTED -> "Open WhatsApp to start chatting.";
            case MATCH_SUGGESTIONS_AVAILABLE -> "There may be useful suggestions available now.";
        };
    }
}
