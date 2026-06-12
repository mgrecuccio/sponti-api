package com.mgrtech.sponti_api.contact.internal.repository;

import com.mgrtech.sponti_api.contact.internal.domain.ContactInvitationEntity;
import com.mgrtech.sponti_api.contact.internal.domain.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactInvitationRepository extends JpaRepository<ContactInvitationEntity, Long> {

    Optional<ContactInvitationEntity> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    Optional<ContactInvitationEntity> findByIdAndRecipientUserIdAndStatus(
            Long id,
            Long recipient,
            InvitationStatus status
    );

    Optional<ContactInvitationEntity> findByIdAndSenderUserIdAndStatus(
            Long id,
            Long senderUserId,
            InvitationStatus status
    );

    boolean existsBySenderUserIdAndRecipientUserIdAndStatus(
            Long senderUserId,
            Long recipientUserId,
            InvitationStatus status
    );

    List<ContactInvitationEntity> findAllByRecipientUserIdAndStatusOrderByCreatedAtDesc(
            Long recipientUserId,
            InvitationStatus status
    );
}
