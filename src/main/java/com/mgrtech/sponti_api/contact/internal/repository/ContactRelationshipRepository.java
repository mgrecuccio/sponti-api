package com.mgrtech.sponti_api.contact.internal.repository;

import com.mgrtech.sponti_api.contact.internal.domain.ContactRelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRelationshipRepository extends JpaRepository<ContactRelationshipEntity, Long> {

    List<ContactRelationshipEntity> findAllByOwnerUserId(Long ownerUserId);
}
