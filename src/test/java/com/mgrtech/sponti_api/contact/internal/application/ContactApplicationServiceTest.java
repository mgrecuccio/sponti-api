package com.mgrtech.sponti_api.contact.internal.application;

import com.mgrtech.sponti_api.contact.internal.domain.ContactInvitationEntity;
import com.mgrtech.sponti_api.contact.internal.domain.ContactRelationshipEntity;
import com.mgrtech.sponti_api.contact.internal.domain.InvitationStatus;
import com.mgrtech.sponti_api.contact.internal.domain.RelationshipStatus;
import com.mgrtech.sponti_api.contact.internal.repository.ContactInvitationRepository;
import com.mgrtech.sponti_api.contact.internal.repository.ContactRelationshipRepository;
import com.mgrtech.sponti_api.user.api.query.UserLookupQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ContactInvitationRepository contactInvitationRepository = mock(ContactInvitationRepository.class);
    private final ContactRelationshipRepository contactRelationshipRepository = mock(ContactRelationshipRepository.class);
    private final UserLookupQuery userLookupQuery = mock(UserLookupQuery.class);
    private final UserProfileQuery userProfileQuery = mock(UserProfileQuery.class);
    private final ContactApplicationService service = new ContactApplicationService(
            contactInvitationRepository,
            contactRelationshipRepository,
            userLookupQuery,
            userProfileQuery,
            CLOCK
    );

    @Test
    void remove_contact_marks_both_relationship_sides_as_removed() {
        var ownerSide = ContactRelationshipEntity.accepted(1L, 2L, "Teammate", NOW);
        var contactSide = ContactRelationshipEntity.accepted(2L, 1L, null, NOW);

        when(contactRelationshipRepository.findByOwnerUserIdAndContactUserId(1L, 2L))
                .thenReturn(Optional.of(ownerSide));
        when(contactRelationshipRepository.findByOwnerUserIdAndContactUserId(2L, 1L))
                .thenReturn(Optional.of(contactSide));

        service.removeContact(1L, 2L);

        assertThat(ownerSide.getRelationshipStatus()).isEqualTo(RelationshipStatus.REMOVED);
        assertThat(contactSide.getRelationshipStatus()).isEqualTo(RelationshipStatus.REMOVED);
    }

    @Test
    void accept_invitation_reuses_existing_relationship_rows_after_removal() {
        var invitation = ContactInvitationEntity.create(1L, 2L, "Teammate again", NOW);
        var senderSide = ContactRelationshipEntity.accepted(1L, 2L, "Teammate", NOW);
        var recipientSide = ContactRelationshipEntity.accepted(2L, 1L, null, NOW);
        senderSide.remove(NOW);
        recipientSide.remove(NOW);

        when(contactInvitationRepository.findByIdAndRecipientUserId(10L, 2L))
                .thenReturn(Optional.of(invitation));
        when(contactRelationshipRepository.existsByOwnerUserIdAndContactUserIdAndRelationshipStatus(
                1L, 2L, RelationshipStatus.ACCEPTED
        )).thenReturn(false);
        when(contactRelationshipRepository.existsByOwnerUserIdAndContactUserIdAndRelationshipStatus(
                1L, 2L, RelationshipStatus.BLOCKED
        )).thenReturn(false);
        when(contactRelationshipRepository.existsByOwnerUserIdAndContactUserIdAndRelationshipStatus(
                2L, 1L, RelationshipStatus.BLOCKED
        )).thenReturn(false);
        when(contactRelationshipRepository.findByOwnerUserIdAndContactUserId(1L, 2L))
                .thenReturn(Optional.of(senderSide));
        when(contactRelationshipRepository.findByOwnerUserIdAndContactUserId(2L, 1L))
                .thenReturn(Optional.of(recipientSide));

        service.acceptInvitation(2L, 10L);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(senderSide.getRelationshipStatus()).isEqualTo(RelationshipStatus.ACCEPTED);
        assertThat(senderSide.getNickname()).isEqualTo("Teammate again");
        assertThat(recipientSide.getRelationshipStatus()).isEqualTo(RelationshipStatus.ACCEPTED);
        verify(contactRelationshipRepository).save(senderSide);
        verify(contactRelationshipRepository).save(recipientSide);
    }
}
