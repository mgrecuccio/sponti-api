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
                @Index(name = "idx_notification_history_user_type_sent", columnList = "user_id,type,sent_at")
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
    }
}
