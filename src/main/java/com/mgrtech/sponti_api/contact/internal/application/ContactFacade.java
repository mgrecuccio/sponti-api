package com.mgrtech.sponti_api.contact.internal.application;

import com.mgrtech.sponti_api.contact.internal.application.command.EditContactCommand;
import com.mgrtech.sponti_api.contact.internal.application.command.SendContactInvitationCommand;
import com.mgrtech.sponti_api.contact.api.query.ContactQuery;
import com.mgrtech.sponti_api.contact.internal.application.view.ContactInvitationView;
import com.mgrtech.sponti_api.contact.api.view.ContactView;

public interface ContactFacade extends ContactQuery {

    ContactInvitationView sendInvitation(Long senderUserId, SendContactInvitationCommand command);

    void cancelInvitation(Long senderUserId, Long invitationId);

    void acceptInvitation(Long recipientUserId, Long invitationId);

    void rejectInvitation(Long recipientUserId, Long invitationId);

    void blockContact(Long ownerUserId, Long contactUserId);

    void removeContact(Long ownerUserId, Long contactUserId);

    ContactView editContact(Long ownerUserId, Long contactUserId, EditContactCommand request);
}
