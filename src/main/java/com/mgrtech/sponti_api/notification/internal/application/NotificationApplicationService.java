package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.NotificationFacade;
import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeliveryStatus;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationHistoryRepository;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
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
    private final NotificationDispatcher dispatcher;
    private final UserMatchingPreferencesQuery userMatchingPreferencesQuery;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(SendNotificationCommand command) {
        var now = Instant.now(clock);
        var relatedMatchId = longValue(command.data(), "relatedMatchId", "matchId");
        var decision = sendDecision(command, relatedMatchId, now);
        var history = new NotificationHistoryEntity(
                command.userId(),
                command.type(),
                longValue(command.data(), "relatedUserId", "initiatorUserId"),
                relatedMatchId,
                now,
                metadata(command)
        );

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

        repository.save(history);
        dispatcher.dispatch(history, command);
    }

    private SendDecision sendDecision(SendNotificationCommand command, Long relatedMatchId, Instant now) {
        if (!canSendByUserPreferences(command)) {
            return SendDecision.suppressed(
                    "user-notification-preferences",
                    "type=%s userId=%d".formatted(command.type(), command.userId())
            );
        }

        if (command.type() == NotificationType.MATCH_PROPOSAL_CREATED) {
            return proposalCreatedSendDecision(command, relatedMatchId);
        }

        if (command.type() == NotificationType.MATCH_SUGGESTIONS_AVAILABLE) {
            return matchingSuggestionsSendDecision(command, now);
        }

        return SendDecision.allowed();
    }

    private boolean canSendByUserPreferences(SendNotificationCommand command) {
        return userMatchingPreferencesQuery.getMatchingPreferences(command.userId())
                .map(preferences -> preferences.pushNotificationsEnabled()
                        && (command.type() != NotificationType.MATCH_SUGGESTIONS_AVAILABLE
                        || preferences.suggestionNotificationsEnabled()))
                .orElse(true);
    }

    private SendDecision proposalCreatedSendDecision(SendNotificationCommand command, Long relatedMatchId) {
        if (relatedMatchId == null) {
            return SendDecision.allowed();
        }

        if (repository.existsByUserIdAndTypeAndRelatedMatchId(
                command.userId(),
                command.type(),
                relatedMatchId
        )) {
            return SendDecision.suppressed(
                    "duplicate-business-key",
                    "type=%s userId=%d relatedMatchId=%d".formatted(command.type(), command.userId(), relatedMatchId)
            );
        }

        return SendDecision.allowed();
    }

    private SendDecision matchingSuggestionsSendDecision(SendNotificationCommand command, Instant now) {
        return repository.findFirstByUserIdAndTypeAndStatusOrderBySentAtDesc(
                        command.userId(),
                        command.type(),
                        NotificationDeliveryStatus.SENT
                )
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

    private String metadata(SendNotificationCommand command) {
        var dataMetadata = metadata(command.data());
        var title = "title=" + command.title();
        var body = "body=" + command.body();
        return dataMetadata == null ? title + ";" + body : title + ";" + body + ";" + dataMetadata;
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
