package com.mgrtech.sponti_api.sms.internal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_verification_entity")
@NoArgsConstructor
@Getter
public class VerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "phone_number", nullable = false, length = 16)
    private String phoneNumber;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationPurpose purpose;

    @Column(name = "attempts")
    private int attempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    public VerificationEntity(
            String phoneNumber,
            Long userId,
            String clientIp,
            VerificationPurpose purpose,
            Instant expiresAt
    ) {
        this.phoneNumber = phoneNumber;
        this.userId = userId;
        this.clientIp = clientIp;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return VerificationStatus.VERIFIED.equals(status);
    }

    public void recordFailedAttempt() {
        this.attempts++;
        this.status = VerificationStatus.ERROR;
    }

    public void markVerified() {
        this.status = VerificationStatus.VERIFIED;
    }
}
