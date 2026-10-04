package com.mgrtech.sponti_api.user.internal.domain;

import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_deletion_tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserDeletionTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserDeletionModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserDeletionTaskStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public UserDeletionTaskEntity(UserEntity user, UserDeletionModule module) {
        this.user = user;
        this.module = module;
        this.status = UserDeletionTaskStatus.PENDING;
    }

    public void markCompleted(Instant now) {
        this.completedAt = now;
        this.lastUpdatedAt = now;
        this.status = UserDeletionTaskStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void markFailed(Instant now, String reason) {
        this.completedAt = null;
        this.lastUpdatedAt = now;
        this.status = UserDeletionTaskStatus.FAILED;
        this.errorMessage = reason;
    }
}
