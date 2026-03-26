package com.mgrtech.sponti_api.contact.internal.exception;

public class ContactInvitationNotFoundException extends RuntimeException {
    public ContactInvitationNotFoundException() {
        super("Contact invitation not found");
    }
}
