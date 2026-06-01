package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.NotificationFacade;
import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationHistoryRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
class NotificationApplicationService implements NotificationFacade {

    private static final Logger log = LoggerFactory.getLogger(NotificationApplicationService.class);

    private final Clock clock;
    private final NotificationProperties properties;
    private final NotificationHistoryRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(SendNotificationCommand command) {
        var now = Instant.now(clock);
        var decision = sendDecision(command, now);

        if (!decision.canSend()) {
            log.info(
                    "Notification suppressed: userId={} type={} reason={} details={}",
                    command.userId(),
                    command.type(),
                    decision.reason(),
                    decision.details()
            );
            return;
        }

        log.info("Placeholder notification for user {}: {}", command.userId(), command.title());
        repository.save(new NotificationHistoryEntity(
                command.userId(),
                command.type(),
                longValue(command.data(), "relatedUserId", "initiatorUserId"),
                longValue(command.data(), "relatedMatchId", "matchId"),
                now,
                metadata(command.data())
        ));
    }

    private SendDecision sendDecision(SendNotificationCommand command, Instant now) {
        if (command.type() != NotificationType.MATCH_SUGGESTIONS_AVAILABLE) {
            return SendDecision.allowed();
        }

        return repository.findFirstByUserIdAndTypeOrderBySentAtDesc(command.userId(), command.type())
                .map(latest -> sendDecision(command, latest, now))
                .orElseGet(SendDecision::allowed);
    }

    private SendDecision sendDecision(
            SendNotificationCommand command,
            NotificationHistoryEntity latest,
            Instant now
    ) {
        var cooldownEndsAt = latest.getSentAt().plus(cooldown(command.type()));
        if (cooldownEndsAt.isAfter(now)) {
            return SendDecision.suppressed(
                    "inside-cooldown",
                    "sentAt=%s cooldownEndsAt=%s now=%s".formatted(latest.getSentAt(), cooldownEndsAt, now)
            );
        }

        var newFingerprint = stringValue(command.data(), "suggestionFingerprint");
        var latestFingerprint = metadataValue(latest.getMetadata(), "suggestionFingerprint");
        if (newFingerprint == null || !newFingerprint.equals(latestFingerprint)) {
            return SendDecision.allowed();
        }

        var newBestScore = intValue(command.data(), "bestScore");
        var latestBestScore = intValue(latest.getMetadata(), "bestScore");
        if (newBestScore > latestBestScore) {
            return SendDecision.allowed();
        }

        return SendDecision.suppressed(
                "duplicate-or-not-improved",
                "fingerprint=%s latestBestScore=%d newBestScore=%d".formatted(
                        newFingerprint,
                        latestBestScore,
                        newBestScore
                )
        );
    }

    private Duration cooldown(NotificationType type) {
        if (type == NotificationType.MATCH_SUGGESTIONS_AVAILABLE) {
            return properties.matchingSuggestionsCooldown();
        }

        return Duration.ZERO;
    }

    private Long longValue(Map<String, String> data, String preferredKey, String fallbackKey) {
        if (data == null) {
            return null;
        }

        var value = data.getOrDefault(preferredKey, data.get(fallbackKey));
        return value != null ? Long.valueOf(value) : null;
    }

    private String metadata(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        return data.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"));
    }

    private String stringValue(Map<String, String> data, String key) {
        return data == null ? null : data.get(key);
    }

    private String metadataValue(String metadata, String key) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }

        var prefix = key + "=";
        for (var part : metadata.split(";")) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length());
            }
        }

        return null;
    }

    private int intValue(Map<String, String> data, String key) {
        var value = stringValue(data, key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private int intValue(String metadata, String key) {
        var value = metadataValue(metadata, key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    private record SendDecision(boolean canSend, String reason, String details) {

        static SendDecision allowed() {
            return new SendDecision(true, null, null);
        }

        static SendDecision suppressed(String reason, String details) {
            return new SendDecision(false, reason, details);
        }
    }
}
