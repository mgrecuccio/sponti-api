package com.mgrtech.sponti_api.contact.internal.exception;

public class ContactNotFoundException extends RuntimeException {
    public ContactNotFoundException() {
        super("Contact not found");
    }
}
