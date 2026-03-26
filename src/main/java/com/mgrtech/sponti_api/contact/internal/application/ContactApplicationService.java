package com.mgrtech.sponti_api.contact.internal.application;

import com.mgrtech.sponti_api.contact.api.*;
import com.mgrtech.sponti_api.contact.internal.domain.ContactInvitationEntity;
import com.mgrtech.sponti_api.contact.internal.domain.ContactRelationshipEntity;
import com.mgrtech.sponti_api.contact.internal.domain.InvitationStatus;
import com.mgrtech.sponti_api.contact.internal.domain.RelationshipStatus;
import com.mgrtech.sponti_api.contact.internal.exception.*;
import com.mgrtech.sponti_api.contact.internal.repository.ContactInvitationRepository;
import com.mgrtech.sponti_api.contact.internal.repository.ContactRelationshipRepository;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.user.api.UserQueryFacade;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.normalizeEmail;

@Service
@Transactional
@AllArgsConstructor
class ContactApplicationService implements ContactFacade {

    private final ContactInvitationRepository contactInvitationRepository;
    private final ContactRelationshipRepository contactRelationshipRepository;
    private final UserQueryFacade userQueryFacade;
    private final Clock clock;

    @Override
    public ContactInvitationView sendInvitation(Long senderUserId, SendContactInvitationCommand command) {
        var now = Instant.now(clock);

        var recipient = userQueryFacade.findByEmailForLookup(normalizeEmail(command.email()))
                .orElseThrow(() -> new UserNotFoundException(command.email()));

        var recipientUserId = recipient.id();

        if(senderUserId.equals(recipientUserId)) {
            throw new CannotInviteSelfException();
        }

        if(hasAnyBlockingRelationship(senderUserId, recipientUserId)) {
            throw new ContactBlockedException();
        }

        if(contactRelationshipRepository.existsByOwnerUserIdAndContactUserIdAndRelationshipStatus(
                senderUserId, recipientUserId, RelationshipStatus.ACCEPTED
        )) {
            throw new ContactAlreadyExistsException();
        }

        boolean pendingInvitationExists =
                contactInvitationRepository.existsBySenderUserIdAndRecipientUserIdAndStatus(
                        senderUserId,
                        recipientUserId,
                        InvitationStatus.PENDING
                );

        if (pendingInvitationExists) {
            throw new ContactInvitationAlreadyExistsException();
        }

        var invitation = ContactInvitationEntity.create(
                senderUserId,
                recipientUserId,
                command.nickname(),
                now
        );

        contactInvitationRepository.save(invitation);

        return new ContactInvitationView(
                invitation.getId(),
                invitation.getSenderUserId(),
                invitation.getRecipientUserId(),
                invitation.getNickName(),
                invitation.getStatusString(),
                invitation.getCreatedAt()
        );
    }

    @Override
    public void acceptInvitation(Long recipientUserId, Long invitationId) {
        var now = Instant.now(clock);

        var invitation = contactInvitationRepository.findByIdAndRecipientUserId(invitationId, recipientUserId)
                .orElseThrow(ContactInvitationNotFoundException::new);

        var senderUserId = invitation.getSenderUserId();

        if (contactRelationshipRepository.findByOwnerUserIdAndContactUserId(senderUserId, recipientUserId).isPresent()
                || contactRelationshipRepository.findByOwnerUserIdAndContactUserId(recipientUserId, senderUserId).isPresent()) {
            throw new ContactAlreadyExistsException();
        }

        if (hasAnyBlockingRelationship(senderUserId, recipientUserId)) {
            throw new ContactBlockedException();
        }

        invitation.accept(now);

        var senderSide = ContactRelationshipEntity.accepted(
                senderUserId,
                recipientUserId,
                invitation.getNickName(),
                now
        );

        var recipientSide = ContactRelationshipEntity.accepted(
                recipientUserId,
                senderUserId,
                null,
                now
        );

        contactRelationshipRepository.save(senderSide);
        contactRelationshipRepository.save(recipientSide);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactView> getAcceptedContacts(Long ownerUserId) {
        return contactRelationshipRepository
                .findAllByOwnerUserIdAndRelationshipStatusOrderByCreatedAtDesc(ownerUserId, RelationshipStatus.ACCEPTED)
                .stream()
                .map(relationship -> new ContactView(
                        relationship.getContactUserId(),
                        relationship.getNickname(),
                        relationship.isFavorite(),
                        relationship.getCreatedAt()
                ))
                .toList();
    }


    @Override
    public void blockContact(Long ownerUserId, Long contactUserId) {
        var now = Instant.now(clock);

        if (ownerUserId.equals(contactUserId)) {
            throw new CannotBlockSelfException();
        }

        var relationship = contactRelationshipRepository
                .findByOwnerUserIdAndContactUserId(ownerUserId, contactUserId)
                .orElseThrow(ContactNotFoundException::new);

        relationship.block(now);
    }

    @Override
    public void removeContact(Long ownerUserId, Long contactUserId) {
        var now = Instant.now(clock);

        if (ownerUserId.equals(contactUserId)) {
            throw new CannotRemoveSelfException();
        }

        var relationship = contactRelationshipRepository
                .findByOwnerUserIdAndContactUserId(ownerUserId, contactUserId)
                .orElseThrow(ContactNotFoundException::new);

        relationship.remove(now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingContactInvitationView> getPendingIncomingInvitations(Long recipientUserId) {
        var senderProfiles = new HashMap<Long, SenderProfile>();

        return contactInvitationRepository
                .findAllByRecipientUserIdAndStatusOrderByCreatedAtDesc(recipientUserId, InvitationStatus.PENDING)
                .stream()
                .map(invitation -> toPendingInvitationView(invitation, senderProfiles))
                .toList();
    }

    private boolean hasAnyBlockingRelationship(Long senderUserId, Long recipientUserId) {
        return contactRelationshipRepository.existsByOwnerUserIdAndContactUserIdAndRelationshipStatus(
                senderUserId, recipientUserId, RelationshipStatus.BLOCKED
        ) || contactRelationshipRepository.existsByOwnerUserIdAndContactUserIdAndRelationshipStatus(
                recipientUserId, senderUserId, RelationshipStatus.BLOCKED
        ) ;
    }

    private PendingContactInvitationView toPendingInvitationView(
            ContactInvitationEntity invitation,
            Map<Long, SenderProfile> senderProfiles
    ) {
        var senderProfile = senderProfiles.computeIfAbsent(
                invitation.getSenderUserId(),
                this::loadSenderProfile
        );

        return new PendingContactInvitationView(
                invitation.getId(),
                invitation.getSenderUserId(),
                senderProfile.email(),
                senderProfile.displayName(),
                invitation.getNickName(),
                invitation.getStatusString(),
                invitation.getCreatedAt()
        );
    }

    private SenderProfile loadSenderProfile(Long senderUserId) {
        return userQueryFacade.getProfileById(senderUserId)
                .map(profile -> new SenderProfile(profile.email(), profile.displayName()))
                .orElseGet(() -> new SenderProfile(null, null));
    }

    private record SenderProfile(String email, String displayName) {}
}
