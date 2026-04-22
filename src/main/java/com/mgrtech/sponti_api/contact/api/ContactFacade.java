package com.mgrtech.sponti_api.contact.api;

import com.mgrtech.sponti_api.contact.internal.web.ContactController;
import jakarta.validation.Valid;

import java.util.List;

public interface ContactFacade {

    ContactInvitationView sendInvitation(Long senderUserId, SendContactInvitationCommand command);

    void acceptInvitation(Long recipientUserId, Long invitationId);

    List<ContactView> getAcceptedContacts(Long ownerUserId);

    void blockContact(Long ownerUserId, Long contactUserId);

    void removeContact(Long ownerUserId, Long contactUserId);

    List<PendingContactInvitationView> getPendingIncomingInvitations(Long recipientUserId);

    ContactView editContact(Long ownerUserId, Long contactUserId, EditContactCommand request);
}
