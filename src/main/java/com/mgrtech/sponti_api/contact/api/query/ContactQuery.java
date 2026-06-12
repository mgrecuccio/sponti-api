package com.mgrtech.sponti_api.contact.api.query;

import com.mgrtech.sponti_api.contact.api.view.ContactView;
import com.mgrtech.sponti_api.contact.api.view.PendingContactInvitationView;

import java.util.List;
import java.util.Optional;

public interface ContactQuery {

    List<ContactView> getAcceptedContacts(Long ownerUserId);

    Optional<ContactView> findAcceptedContact(Long userId, Long candidateUserId);

    List<PendingContactInvitationView> getPendingIncomingInvitations(Long recipientUserId);
}
