package com.mgrtech.sponti_api.contact.internal.exception;

public class ContactInviteeNotFoundException extends RuntimeException {
    public ContactInviteeNotFoundException() {
        super("No account exists for that phone number.");
    }
}
