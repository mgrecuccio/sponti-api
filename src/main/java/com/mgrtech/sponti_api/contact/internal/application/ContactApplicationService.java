package com.mgrtech.sponti_api.contact.internal.application;

import com.mgrtech.sponti_api.contact.api.ContactFacade;
import com.mgrtech.sponti_api.contact.api.dto.ContactView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ContactApplicationService implements ContactFacade {

    @Override
    public List<ContactView> listAcceptedContacts(Long ownerUserId) {
        return List.of();
    }
}
