package com.mgrtech.sponti_api.contact.internal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "contact_invitations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contact_invitation_sender_recipient_pending",
                        columnNames = {"sender_user_id", "recipient_user_id", "status"}
                )
        }
)
@NoArgsConstructor
@Getter
public class ContactInvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "nickname", length = 100)
    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "responded_at")
    @UpdateTimestamp
    private Instant respondedAt;

    private ContactInvitationEntity(
            Long senderUserId,
            Long recipientUserId,
            String nickname,
            InvitationStatus status,
            Instant createdAt
    ) {
        if (senderUserId.equals(recipientUserId)) {
            throw new IllegalArgumentException("Sender and recipient must be different users");
        }

        this.senderUserId = Objects.requireNonNull(senderUserId);
        this.recipientUserId = Objects.requireNonNull(recipientUserId);
        this.nickName = nickname;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static ContactInvitationEntity create(
            Long senderUserId,
            Long recipientUserId,
            String nickname,
            Instant now
    ) {
        return new ContactInvitationEntity(
                senderUserId,
                recipientUserId,
                nickname,
                InvitationStatus.PENDING,
                now
        );
    }

    public void accept(Instant now) {
        ensurePending();
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = now;
    }

    public void reject(Instant now) {
        ensurePending();
        this.status = InvitationStatus.REJECTED;
        this.respondedAt = now;
    }

    public void cancel(Instant now) {
        ensurePending();
        this.status = InvitationStatus.CANCELLED;
        this.respondedAt = now;
    }

    public boolean isPending() {
        return InvitationStatus.PENDING.equals(this.status);
    }

    private void ensurePending() {
        if (!isPending()) {
            throw new IllegalStateException("Invitation is not pending");
        }
    }

    public String getStatusString() {
        return this.status.name();
    }


}
