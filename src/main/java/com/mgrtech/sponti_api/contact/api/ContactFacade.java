package com.mgrtech.sponti_api.contact.api;

import java.util.List;

public interface ContactFacade {

    ContactInvitationView sendInvitation(Long senderUserId, SendContactInvitationCommand command);

    void acceptInvitation(Long recipientUserId, Long invitationId);

    List<ContactView> getAcceptedContacts(Long ownerUserId);

    void blockContact(Long ownerUserId, Long contactUserId);

    void removeContact(Long ownerUserId, Long contactUserId);

    List<PendingContactInvitationView> getPendingIncomingInvitations(Long recipientUserId);

}
