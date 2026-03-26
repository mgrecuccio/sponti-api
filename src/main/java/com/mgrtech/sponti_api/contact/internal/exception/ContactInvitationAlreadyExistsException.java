package com.mgrtech.sponti_api.contact.internal.exception;

public class ContactInvitationAlreadyExistsException extends RuntimeException {
    public ContactInvitationAlreadyExistsException() {
        super("A pending contact invitation already exists");
    }
}
