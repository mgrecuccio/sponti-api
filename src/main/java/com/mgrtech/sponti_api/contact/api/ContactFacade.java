package com.mgrtech.sponti_api.contact.api;

import com.mgrtech.sponti_api.contact.api.dto.ContactView;

import java.util.List;

public interface ContactFacade {

    List<ContactView> listAcceptedContacts(Long ownerUserId);
}
