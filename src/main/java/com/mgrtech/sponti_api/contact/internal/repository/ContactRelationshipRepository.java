package com.mgrtech.sponti_api.contact.internal.repository;

import com.mgrtech.sponti_api.contact.internal.domain.ContactRelationshipEntity;
import com.mgrtech.sponti_api.contact.internal.domain.InvitationStatus;
import com.mgrtech.sponti_api.contact.internal.domain.RelationshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContactRelationshipRepository extends JpaRepository<ContactRelationshipEntity, Long> {

    List<ContactRelationshipEntity> findAllByOwnerUserIdAndRelationshipStatusOrderByCreatedAtDesc(
            Long ownerUserId,
            RelationshipStatus relationshipStatus
    );

    Optional<ContactRelationshipEntity> findByOwnerUserIdAndContactUserId(
            Long ownerUserId,
            Long contactUserId
    );

    boolean existsByOwnerUserIdAndContactUserIdAndRelationshipStatus(
            Long ownerUserId,
            Long contactUserId,
            RelationshipStatus relationshipStatus
    );
}
