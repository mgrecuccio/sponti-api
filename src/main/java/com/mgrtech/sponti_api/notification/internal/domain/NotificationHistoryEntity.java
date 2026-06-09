package com.mgrtech.sponti_api.notification.internal.domain;

import com.mgrtech.sponti_api.notification.api.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "notification_history",
        indexes = {
                @Index(name = "idx_notification_history_user_type_sent", columnList = "user_id,type,sent_at"),
                @Index(name = "idx_notification_history_retry", columnList = "status,next_retry_at")
        }
)
@NoArgsConstructor
@Getter
public class NotificationHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private NotificationType type;

    @Column(name = "related_user_id")
    private Long relatedUserId;

    @Column(name = "related_match_id")
    private Long relatedMatchId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(columnDefinition = "text")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationDeliveryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private NotificationProvider provider;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "failure_code", length = 120)
    private String failureCode;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    public NotificationHistoryEntity(
            Long userId,
            NotificationType type,
            Long relatedUserId,
            Long relatedMatchId,
            Instant sentAt,
            String metadata
    ) {
        this.userId = userId;
        this.type = type;
        this.relatedUserId = relatedUserId;
        this.relatedMatchId = relatedMatchId;
        this.sentAt = sentAt;
        this.metadata = metadata;
        this.status = NotificationDeliveryStatus.PENDING;
        this.attemptCount = 0;
    }

    public void markSent(NotificationProvider provider, String providerMessageId, Instant now) {
        this.status = NotificationDeliveryStatus.SENT;
        this.provider = provider;
        this.providerMessageId = providerMessageId;
        this.failureCode = null;
        this.failureReason = null;
        this.nextRetryAt = null;
        this.incrementAttemptFields(now);
    }

    public void markNoDeviceToken(Instant now) {
        this.status = NotificationDeliveryStatus.NO_DEVICE_TOKEN;
        this.failureCode = "NO_DEVICE_TOKEN";
        this.failureReason = "No enabled device token found for user.";
        this.nextRetryAt = null;
        this.incrementAttemptFields(now);
    }

    public void markFailed(String failureCode, String failureReason, Instant now) {
        this.status = NotificationDeliveryStatus.FAILED;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.nextRetryAt = null;
        this.incrementAttemptFields(now);
    }

    public void markRetryPending(
            String failureCode,
            String failureReason,
            Instant now,
            Instant nextRetryAt
    ) {
        this.status = NotificationDeliveryStatus.RETRY_PENDING;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.nextRetryAt = nextRetryAt;
        this.incrementAttemptFields(now);
    }

    private void incrementAttemptFields(Instant now) {
        this.lastAttemptAt = now;
        this.attemptCount++;
    }
}
