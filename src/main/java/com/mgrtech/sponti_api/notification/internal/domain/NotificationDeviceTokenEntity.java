package com.mgrtech.sponti_api.notification.internal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "notification_device_tokens",
        indexes = {
                @Index(name = "idx_notification_device_tokens_user_enabled", columnList = "user_id,enabled")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_device_tokens_token", columnNames = "token")
        }
)
@NoArgsConstructor
@Getter
public class NotificationDeviceTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(nullable = false, columnDefinition = "text")
    private String token;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "app_version", length = 60)
    private String appVersion;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationDeviceTokenEntity(
            Long userId,
            DevicePlatform platform,
            String token,
            String deviceId,
            String appVersion,
            Instant now
    ) {
        this.userId = userId;
        this.platform = platform;
        this.token = token;
        this.deviceId = deviceId;
        this.appVersion = appVersion;
        this.enabled = true;
        this.lastSeenAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void refresh(
            Long userId,
            DevicePlatform platform,
            String deviceId,
            String appVersion,
            Instant now
    ) {
        this.userId = userId;
        this.platform = platform;
        this.deviceId = deviceId;
        this.appVersion = appVersion;
        this.enabled = true;
        this.lastSeenAt = now;
        this.updatedAt = now;
    }

    public void disable(Instant now) {
        this.enabled = false;
        this.updatedAt = now;
    }
}
