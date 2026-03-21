package com.mgrtech.sponti_api.contact.internal.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contact_relationships",
       uniqueConstraints = @UniqueConstraint(name = "uk_contact_owner_contact", columnNames = {"owner_user_id", "contact_user_id"}))
public class ContactRelationshipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "contact_user_id", nullable = false)
    private Long contactUserId;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "favorite", nullable = false)
    private boolean favorite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipStatus status = RelationshipStatus.ACCEPTED;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
