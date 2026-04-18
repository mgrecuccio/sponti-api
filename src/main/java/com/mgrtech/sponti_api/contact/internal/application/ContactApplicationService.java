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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ContactApplicationService.class);

    private final ContactInvitationRepository contactInvitationRepository;
    private final ContactRelationshipRepository contactRelationshipRepository;
    private final UserQueryFacade userQueryFacade;
    private final Clock clock;

    @Override
    public ContactInvitationView sendInvitation(Long senderUserId, SendContactInvitationCommand command) {
        log.info("Send contact invitation requested: senderUserId={}", senderUserId);
        var now = Instant.now(clock);

        var recipient = userQueryFacade.findByEmailForLookup(normalizeEmail(command.email()))
                .orElseThrow(() -> {
                    log.warn("Send invitation failed: recipient not found for senderUserId={}", senderUserId);
                    return new UserNotFoundException(
                            "No account exists for that email address yet. You can invite only existing users."
                    );
                });

        var recipientUserId = recipient.id();

        if(senderUserId.equals(recipientUserId)) {
            log.warn("Send invitation blocked: cannot invite self userId={}", senderUserId);
            throw new CannotInviteSelfException();
        }

        if(hasAnyBlockingRelationship(senderUserId, recipientUserId)) {
            log.warn("Send invitation blocked by relationship: senderUserId={} recipientUserId={}", senderUserId, recipientUserId);
            throw new ContactBlockedException();
        }

        if(contactRelationshipRepository.existsByOwnerUserIdAndContactUserIdAndRelationshipStatus(
                senderUserId, recipientUserId, RelationshipStatus.ACCEPTED
        )) {
            log.warn("Send invitation blocked: relationship already exists for senderUserId={} and recipientUserId={}", senderUserId, recipientUserId);
            throw new ContactAlreadyExistsException();
        }

        boolean pendingInvitationExists =
                contactInvitationRepository.existsBySenderUserIdAndRecipientUserIdAndStatus(
                        senderUserId,
                        recipientUserId,
                        InvitationStatus.PENDING
                );

        if (pendingInvitationExists) {
            log.warn("Send invitation blocked: pending invitation already exists for senderUserId={} and recipientUserId={}", senderUserId, recipientUserId);
            throw new ContactInvitationAlreadyExistsException();
        }

        var invitation = ContactInvitationEntity.create(
                senderUserId,
                recipientUserId,
                command.nickname(),
                now
        );

        contactInvitationRepository.save(invitation);
        log.info("Contact invitation created: invitationId={} senderUserId={} recipientUserId={}", invitation.getId(), senderUserId, recipientUserId);

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
        log.info("Accept contact invitation requested: senderUserId={} , invitationId={}", recipientUserId, invitationId);
        var now = Instant.now(clock);

        var invitation = contactInvitationRepository.findByIdAndRecipientUserId(invitationId, recipientUserId)
                .orElseThrow(ContactInvitationNotFoundException::new);

        var senderUserId = invitation.getSenderUserId();

        if (contactRelationshipRepository.findByOwnerUserIdAndContactUserId(senderUserId, recipientUserId).isPresent()
                || contactRelationshipRepository.findByOwnerUserIdAndContactUserId(recipientUserId, senderUserId).isPresent()) {
            log.warn("Accepting invitation blocked: relationship already exists for senderUserId={} and recipientUserId={}", senderUserId, recipientUserId);
            throw new ContactAlreadyExistsException();
        }

        if (hasAnyBlockingRelationship(senderUserId, recipientUserId)) {
            log.warn("Accepting invitation blocked by relationship: senderUserId={} recipientUserId={}", senderUserId, recipientUserId);
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
        log.info("Invitation accepted and relationships created: senderUserId={} invitationId={}", senderUserId, invitationId);
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
        log.info("Block contact requested: ownerUserId={} , contactUserId={}", ownerUserId, contactUserId);
        var now = Instant.now(clock);

        if (ownerUserId.equals(contactUserId)) {
            log.warn("Users cannot block themselves: ownerUserId={} contactUserId={}", ownerUserId, contactUserId);
            throw new CannotBlockSelfException();
        }

        var relationship = contactRelationshipRepository
                .findByOwnerUserIdAndContactUserId(ownerUserId, contactUserId)
                .orElseThrow(ContactNotFoundException::new);

        relationship.block(now);
        log.info("OwnerUserId={} blocked contactUserId={}", ownerUserId, contactUserId);
    }

    @Override
    public void removeContact(Long ownerUserId, Long contactUserId) {
        log.info("Remove contact requested: ownerUserId={} , contactUserId={}", ownerUserId, contactUserId);
        var now = Instant.now(clock);

        if (ownerUserId.equals(contactUserId)) {
            log.warn("Users cannot remove themselves: ownerUserId={} contactUserId={}", ownerUserId, contactUserId);
            throw new CannotRemoveSelfException();
        }

        var relationship = contactRelationshipRepository
                .findByOwnerUserIdAndContactUserId(ownerUserId, contactUserId)
                .orElseThrow(ContactNotFoundException::new);

        relationship.remove(now);
        log.info("OwnerUserId={} removed contactUserId={}", ownerUserId, contactUserId);
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
