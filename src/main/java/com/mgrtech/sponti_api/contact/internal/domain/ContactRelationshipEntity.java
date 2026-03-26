package com.mgrtech.sponti_api.contact.internal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "contact_relationships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contact_relationship_owner_contact",
                        columnNames = {"owner_user_id", "contact_user_id"}
                )
        }
)
@NoArgsConstructor
@Getter
public class ContactRelationshipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private Long ownerUserId;

    @Column(name = "contact_user_id", nullable = false, updatable = false)
    private Long contactUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_status", nullable = false, length = 20)
    private RelationshipStatus relationshipStatus;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "favorite", nullable = false)
    private boolean favorite;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private ContactRelationshipEntity(
            Long ownerUserId,
            Long contactUserId,
            RelationshipStatus relationshipStatus,
            String nickname,
            boolean favorite,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (ownerUserId.equals(contactUserId)) {
            throw new IllegalArgumentException("Owner and contact must be different users");
        }

        this.ownerUserId = Objects.requireNonNull(ownerUserId);
        this.contactUserId = Objects.requireNonNull(contactUserId);
        this.relationshipStatus = Objects.requireNonNull(relationshipStatus);
        this.nickname = nickname;
        this.favorite = favorite;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static ContactRelationshipEntity accepted(
            Long ownerUserId,
            Long contactUserId,
            String nickname,
            Instant now
    ) {
        return new ContactRelationshipEntity(
                ownerUserId,
                contactUserId,
                RelationshipStatus.ACCEPTED,
                nickname,
                false,
                now,
                now
        );
    }

    public void block(Instant now) {
        this.relationshipStatus = RelationshipStatus.BLOCKED;
        this.updatedAt = now;
    }

    public void remove(Instant now) {
        this.relationshipStatus = RelationshipStatus.REMOVED;
        this.updatedAt = now;
    }

    public boolean isAccepted() {
        return RelationshipStatus.ACCEPTED.equals(this.relationshipStatus);
    }

    public boolean isBlocked() {
        return RelationshipStatus.BLOCKED.equals(this.relationshipStatus);
    }

    public boolean isRemoved() {
        return RelationshipStatus.REMOVED.equals(this.relationshipStatus);
    }
}
