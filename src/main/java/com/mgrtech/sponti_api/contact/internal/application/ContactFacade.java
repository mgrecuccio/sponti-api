package com.mgrtech.sponti_api.contact.internal.application;

import com.mgrtech.sponti_api.contact.internal.application.command.EditContactCommand;
import com.mgrtech.sponti_api.contact.internal.application.command.SendContactInvitationCommand;
import com.mgrtech.sponti_api.contact.internal.application.view.ContactInvitationView;
import com.mgrtech.sponti_api.contact.api.view.ContactView;
import com.mgrtech.sponti_api.contact.api.view.PendingContactInvitationView;

import java.util.List;
import java.util.Optional;

public interface ContactFacade {

    List<ContactView> getAcceptedContacts(Long ownerUserId);

    Optional<ContactView> findAcceptedContact(Long userId, Long candidateUserId);

    List<PendingContactInvitationView> getPendingIncomingInvitations(Long recipientUserId);

    ContactInvitationView sendInvitation(Long senderUserId, SendContactInvitationCommand command);

    void cancelInvitation(Long senderUserId, Long invitationId);

    void acceptInvitation(Long recipientUserId, Long invitationId);

    void rejectInvitation(Long recipientUserId, Long invitationId);

    void blockContact(Long ownerUserId, Long contactUserId);

    void unblockContact(Long ownerUserId, Long contactUserId);

    List<ContactView> getBlockedContacts(Long ownerUserId);

    void removeContact(Long ownerUserId, Long contactUserId);

    ContactView editContact(Long ownerUserId, Long contactUserId, EditContactCommand request);
}
