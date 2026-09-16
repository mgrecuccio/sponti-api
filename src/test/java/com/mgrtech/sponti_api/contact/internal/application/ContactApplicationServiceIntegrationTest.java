package com.mgrtech.sponti_api.contact.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.FullIntegrationTest;
import com.mgrtech.sponti_api.contact.api.view.ContactView;
import com.mgrtech.sponti_api.contact.internal.application.command.EditContactCommand;
import com.mgrtech.sponti_api.contact.internal.application.command.SendContactInvitationCommand;
import com.mgrtech.sponti_api.contact.internal.exception.*;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FullIntegrationTest
class ContactApplicationServiceIntegrationTest {

    private int phoneNumberSequence = 1;

    @Autowired
    ContactFacade contactFacade;

    @Autowired
    UserRegistrationFacade userRegistrationFacade;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    void send_and_accept_invitation_creates_bidirectional_accepted_contacts() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC")
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC")
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );

        assertThat(invitation.id()).isNotNull();
        assertThat(invitation.senderUserId()).isEqualTo(sender.id());
        assertThat(invitation.recipientUserId()).isEqualTo(recipient.id());
        assertThat(invitation.nickName()).isEqualTo("Teammate");
        assertThat(invitation.status()).isEqualTo("PENDING");

        contactFacade.acceptInvitation(recipient.id(), invitation.id());

        var senderContacts = contactFacade.getAcceptedContacts(sender.id());
        assertThat(senderContacts).hasSize(1);
        assertThat(senderContacts.getFirst().contactUserId()).isEqualTo(recipient.id());
        assertThat(senderContacts.getFirst().nickName()).isEqualTo("Teammate");
        assertThat(senderContacts.getFirst().displayName()).isEqualTo("Recipient");
        assertThat(senderContacts.getFirst().favorite()).isFalse();

        var recipientContacts = contactFacade.getAcceptedContacts(recipient.id());
        assertThat(recipientContacts).hasSize(1);
        assertThat(recipientContacts.getFirst().contactUserId()).isEqualTo(sender.id());
        assertThat(recipientContacts.getFirst().nickName()).isNull();
        assertThat(recipientContacts.getFirst().displayName()).isEqualTo("Sender");
        assertThat(recipientContacts.getFirst().favorite()).isFalse();
    }

    @Test
    void send_invitation_throws_when_recipient_user_does_not_exist() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand("+32479999999", "Ghost")
        )).isInstanceOf(ContactInviteeNotFoundException.class);
    }

    @Test
    void send_invitation_throws_exception_when_sender_invites_self() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(sender.phoneNumber(), "Self")
        )).isInstanceOf(CannotInviteSelfException.class);
    }

    @Test
    void block_contact_throws_exception_when_sender_blocks_self() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> contactFacade.blockContact(
                sender.id(),
                sender.id()
        )).isInstanceOf(CannotBlockSelfException.class);
    }

    @Test
    void unblock_contact_throws_exception_when_sender_unblocks_self() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "unblock-self@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> contactFacade.unblockContact(
                sender.id(),
                sender.id()
        )).isInstanceOf(CannotUnblockSelfException.class);
    }

    @Test
    void remove_contact_throws_exception_when_sender_removes_self() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> contactFacade.removeContact(
                sender.id(),
                sender.id()
        )).isInstanceOf(CannotRemoveSelfException.class);
    }

    @Test
    void edit_contact_throws_exception_when_sender_edits_self() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> contactFacade.editContact(
                sender.id(),
                sender.id(),
                new EditContactCommand("Self", true)
        )).isInstanceOf(CannotEditSelfContactException.class);
    }

    @Test
    void send_invitation_rejects_duplicate_pending_invitation() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "First")
        );

        assertThatThrownBy(() -> contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Second")
        )).isInstanceOf(ContactInvitationAlreadyExistsException.class);
    }

    @Test
    void cancel_invitation_marks_pending_invitation_as_cancelled_and_allows_resend() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "cancel-sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "cancel-recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );

        contactFacade.cancelInvitation(sender.id(), invitation.id());

        assertThat(contactFacade.getPendingIncomingInvitations(recipient.id())).isEmpty();

        var resentInvitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate again")
        );

        assertThat(resentInvitation.id()).isNotEqualTo(invitation.id());
        assertThat(resentInvitation.status()).isEqualTo("PENDING");
    }

    @Test
    void cancel_invitation_throws_when_invitation_does_not_belong_to_sender() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "cancel-owner@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "cancel-owner-recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );
        var stranger = userRegistrationFacade.createUser(
                createUserCommand(
                        "cancel-stranger@example.com",
                        "hash",
                        "Stranger",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );

        assertThatThrownBy(() -> contactFacade.cancelInvitation(stranger.id(), invitation.id()))
                .isInstanceOf(ContactInvitationNotFoundException.class);
    }

    @Test
    void cancel_invitation_throws_when_invitation_is_not_pending() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "cancel-accepted-sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "cancel-accepted-recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );
        contactFacade.acceptInvitation(recipient.id(), invitation.id());

        assertThatThrownBy(() -> contactFacade.cancelInvitation(sender.id(), invitation.id()))
                .isInstanceOf(ContactInvitationNotFoundException.class);
    }

    @Test
    void reject_invitation_marks_pending_invitation_as_rejected_and_allows_resend() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "reject-sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "reject-recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );

        contactFacade.rejectInvitation(recipient.id(), invitation.id());

        assertThat(contactFacade.getPendingIncomingInvitations(recipient.id())).isEmpty();
        assertThat(contactFacade.getAcceptedContacts(sender.id())).isEmpty();
        assertThat(contactFacade.getAcceptedContacts(recipient.id())).isEmpty();

        var resentInvitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate again")
        );

        assertThat(resentInvitation.id()).isNotEqualTo(invitation.id());
        assertThat(resentInvitation.status()).isEqualTo("PENDING");
    }

    @Test
    void reject_invitation_throws_when_invitation_does_not_belong_to_recipient() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "reject-owner@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "reject-owner-recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );
        var stranger = userRegistrationFacade.createUser(
                createUserCommand(
                        "reject-stranger@example.com",
                        "hash",
                        "Stranger",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );

        assertThatThrownBy(() -> contactFacade.rejectInvitation(stranger.id(), invitation.id()))
                .isInstanceOf(ContactInvitationNotFoundException.class);
        assertThatThrownBy(() -> contactFacade.rejectInvitation(sender.id(), invitation.id()))
                .isInstanceOf(ContactInvitationNotFoundException.class);
    }

    @Test
    void reject_invitation_throws_when_invitation_is_not_pending() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "reject-accepted-sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "reject-accepted-recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );
        contactFacade.acceptInvitation(recipient.id(), invitation.id());

        assertThatThrownBy(() -> contactFacade.rejectInvitation(recipient.id(), invitation.id()))
                .isInstanceOf(ContactInvitationNotFoundException.class);
    }

    @Test
    void send_invitation_throws_when_contact_is_already_accepted() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );
        contactFacade.acceptInvitation(recipient.id(), invitation.id());

        assertThatThrownBy(() -> contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Again")
        )).isInstanceOf(ContactAlreadyExistsException.class);
    }

    @Test
    void accept_invitation_throws_when_invitation_does_not_belong_to_recipient() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );
        var stranger = userRegistrationFacade.createUser(
                createUserCommand(
                        "stranger@example.com",
                        "hash",
                        "Stranger",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );

        assertThatThrownBy(() -> contactFacade.acceptInvitation(stranger.id(), invitation.id()))
                .isInstanceOf(ContactInvitationNotFoundException.class);
    }

    @Test
    void accept_invitation_throws_when_relationship_is_blocked() {
        var userA = userRegistrationFacade.createUser(
                createUserCommand(
                        "a@example.com",
                        "hash",
                        "A",
                        "UTC"
                )
        );
        var userB = userRegistrationFacade.createUser(
                createUserCommand(
                        "b@example.com",
                        "hash",
                        "B",
                        "UTC"
                )
        );

        var firstInvitation = contactFacade.sendInvitation(
                userA.id(),
                new SendContactInvitationCommand(userB.phoneNumber(), "B")
        );

        contactFacade.acceptInvitation(userB.id(), firstInvitation.id());
        contactFacade.blockContact(userA.id(), userB.id());

        assertThatThrownBy(() -> contactFacade.sendInvitation(
                userB.id(),
                new SendContactInvitationCommand(userA.phoneNumber(), "A")
        )).isInstanceOf(ContactBlockedException.class);
    }

    @Test
    void remove_contact_hides_it_from_accepted_contacts() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );
        contactFacade.acceptInvitation(recipient.id(), invitation.id());

        contactFacade.removeContact(sender.id(), recipient.id());

        assertThat(contactFacade.getAcceptedContacts(sender.id())).isEmpty();
        assertThat(contactFacade.getAcceptedContacts(recipient.id())).hasSize(1);
    }

    @Test
    void block_contact_hides_effective_accepted_contact_for_both_users() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "block-sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "block-recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );
        contactFacade.acceptInvitation(recipient.id(), invitation.id());

        contactFacade.blockContact(sender.id(), recipient.id());

        assertThat(contactFacade.getAcceptedContacts(sender.id())).isEmpty();
        assertThat(contactFacade.getAcceptedContacts(recipient.id())).isEmpty();
        assertThat(contactFacade.getBlockedContacts(sender.id()))
                .extracting(ContactView::contactUserId)
                .containsExactly(recipient.id());
        assertThat(contactFacade.getBlockedContacts(recipient.id())).isEmpty();
        assertThat(contactFacade.findAcceptedContact(sender.id(), recipient.id())).isEmpty();
        assertThat(contactFacade.findAcceptedContact(recipient.id(), sender.id())).isEmpty();
    }

    @Test
    void unblock_contact_restores_effective_accepted_contact_for_both_users() {
        var sender = userRegistrationFacade.createUser(
                createUserCommand(
                        "unblock-sender@example.com",
                        "hash",
                        "Sender",
                        "UTC"
                )
        );
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "unblock-recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                sender.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Teammate")
        );
        contactFacade.acceptInvitation(recipient.id(), invitation.id());
        contactFacade.blockContact(sender.id(), recipient.id());

        contactFacade.unblockContact(sender.id(), recipient.id());

        assertThat(contactFacade.getAcceptedContacts(sender.id())).hasSize(1);
        assertThat(contactFacade.getAcceptedContacts(recipient.id())).hasSize(1);
        assertThat(contactFacade.getBlockedContacts(sender.id())).isEmpty();
        assertThat(contactFacade.getBlockedContacts(recipient.id())).isEmpty();
        assertThat(contactFacade.findAcceptedContact(sender.id(), recipient.id())).isPresent();
        assertThat(contactFacade.findAcceptedContact(recipient.id(), sender.id())).isPresent();
    }

    @Test
    void edit_contact_changes_nick_name_and_favorite_flag() {
        var userA = userRegistrationFacade.createUser(
                createUserCommand(
                        "a@example.com",
                        "hash",
                        "A",
                        "UTC"
                )
        );
        var userB = userRegistrationFacade.createUser(
                createUserCommand(
                        "b@example.com",
                        "hash",
                        "B",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                userA.id(),
                new SendContactInvitationCommand(userB.phoneNumber(), "B")
        );

        contactFacade.acceptInvitation(userB.id(), invitation.id());

        var aContacts = contactFacade.getAcceptedContacts(userA.id());
        assertThat(aContacts).isNotEmpty().hasSize(1);

        // by default, contacts aren't favorite
        assertThat((aContacts.getFirst().favorite())).isFalse();

        var nickNameUsedByA = contactFacade.getAcceptedContacts(userA.id()).getFirst().nickName();
        assertThat(nickNameUsedByA).isEqualTo("B");

        var bContacts = contactFacade.getAcceptedContacts(userB.id());
        assertThat(bContacts).isNotEmpty().hasSize(1);

        // by default, contacts aren't favorite
        assertThat((bContacts.getFirst().favorite())).isFalse();

        var nickNameUsedByB = contactFacade.getAcceptedContacts(userB.id()).getFirst().nickName();
        assertThat(nickNameUsedByB).isNull();

        contactFacade.editContact(userB.id(), userA.id(), new EditContactCommand("A", true));

        bContacts = contactFacade.getAcceptedContacts(userB.id());
        nickNameUsedByB = bContacts.getFirst().nickName();
        assertThat(nickNameUsedByB).isEqualTo("A");
        assertThat((bContacts.getFirst().favorite())).isTrue();
    }

    @Test
    void edit_contact_preserves_favorite_flag_when_favorite_is_null() {
        var userA = userRegistrationFacade.createUser(
                createUserCommand(
                        "preserve-a@example.com",
                        "hash",
                        "Preserve A",
                        "UTC"
                )
        );
        var userB = userRegistrationFacade.createUser(
                createUserCommand(
                        "preserve-b@example.com",
                        "hash",
                        "Preserve B",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                userA.id(),
                new SendContactInvitationCommand(userB.phoneNumber(), "B")
        );
        contactFacade.acceptInvitation(userB.id(), invitation.id());

        contactFacade.editContact(userB.id(), userA.id(), new EditContactCommand("A", true));
        contactFacade.editContact(userB.id(), userA.id(), new EditContactCommand("A updated", null));

        var bContacts = contactFacade.getAcceptedContacts(userB.id());
        assertThat(bContacts.getFirst().nickName()).isEqualTo("A updated");
        assertThat(bContacts.getFirst().favorite()).isTrue();
    }

    @Test
    void get_pending_incoming_invitations_returns_only_pending_for_recipient_in_desc_order() {
        var recipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "recipient@example.com",
                        "hash",
                        "Recipient",
                        "UTC"
                )
        );
        var senderOne = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender-one@example.com",
                        "hash",
                        "Sender One",
                        "UTC"
                )
        );
        var senderTwo = userRegistrationFacade.createUser(
                createUserCommand(
                        "sender-two@example.com",
                        "hash",
                        "Sender Two",
                        "UTC"
                )
        );
        var otherRecipient = userRegistrationFacade.createUser(
                createUserCommand(
                        "other@example.com",
                        "hash",
                        "Other",
                        "UTC"
                )
        );

        var acceptedInvitation = contactFacade.sendInvitation(
                senderOne.id(),
                new SendContactInvitationCommand(recipient.phoneNumber(), "Old teammate")
        );
        contactFacade.acceptInvitation(recipient.id(), acceptedInvitation.id());

        var pendingFromSenderOne = contactFacade.sendInvitation(
                senderOne.id(),
                new SendContactInvitationCommand(otherRecipient.phoneNumber(), "New teammate One")
        );

        var pendingFromSenderTwo = contactFacade.sendInvitation(
                senderTwo.id(),
                new SendContactInvitationCommand(otherRecipient.phoneNumber(), "New teammate Two")
        );

        var pending = contactFacade.getPendingIncomingInvitations(otherRecipient.id());

        assertThat(pending).hasSize(2);
        assertThat(pending.getFirst().invitationId()).isEqualTo(pendingFromSenderTwo.id());
        assertThat(pending.getFirst().senderUserId()).isEqualTo(senderTwo.id());
        assertThat(pending.getFirst().senderEmail()).isEqualTo(senderTwo.email());
        assertThat(pending.getFirst().senderDisplayName()).isEqualTo(senderTwo.displayName());
        assertThat(pending.getFirst().status()).isEqualTo("PENDING");

        assertThat(pending.get(1).invitationId()).isEqualTo(pendingFromSenderOne.id());
        assertThat(pending.get(1).senderUserId()).isEqualTo(senderOne.id());
        assertThat(pending.get(1).senderEmail()).isEqualTo(senderOne.email());
        assertThat(pending.get(1).senderDisplayName()).isEqualTo(senderOne.displayName());
        assertThat(pending.get(1).status()).isEqualTo("PENDING");
    }

    @Test
    void find_accepted_contact_returns_null_if_relationship_does_not_exist() {
        var userA = userRegistrationFacade.createUser(
                createUserCommand(
                        "a@example.com",
                        "hash",
                        "A",
                        "UTC"
                )
        );
        var userB = userRegistrationFacade.createUser(
                createUserCommand(
                        "b@example.com",
                        "hash",
                        "B",
                        "UTC"
                )
        );

        contactFacade.sendInvitation(
                userA.id(),
                new SendContactInvitationCommand(userB.phoneNumber(), "B")
        );

        var acceptedContact = contactFacade.findAcceptedContact(userA.id(), userB.id());
        assertThat(acceptedContact).isEmpty();
    }

    @Test
    void find_accepted_contact() {
        var userA = userRegistrationFacade.createUser(
                createUserCommand(
                        "a@example.com",
                        "hash",
                        "A",
                        "UTC"
                )
        );
        var userB = userRegistrationFacade.createUser(
                createUserCommand(
                        "b@example.com",
                        "hash",
                        "B",
                        "UTC"
                )
        );

        var invitation = contactFacade.sendInvitation(
                userA.id(),
                new SendContactInvitationCommand(userB.phoneNumber(), "B")
        );

        contactFacade.acceptInvitation(userB.id(), invitation.id());

        var acceptedContact = contactFacade.findAcceptedContact(userA.id(), userB.id());
        assertThat(acceptedContact).isNotEmpty();
        assertThat(acceptedContact.get().contactUserId()).isEqualTo(userB.id());
    }

    private CreateUserCommand createUserCommand(String email, String passwordHash, String displayName, String timezone) {
        return new CreateUserCommand(email, passwordHash, displayName, nextPhoneNumber(), timezone);
    }

    private String nextPhoneNumber() {
        return "+32470" + String.format("%06d", phoneNumberSequence++);
    }
}
