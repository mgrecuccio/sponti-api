package com.mgrtech.sponti_api.contact.internal.exception;

public class ContactBlockedException extends RuntimeException {
    public ContactBlockedException() {
        super("Contact interaction is blocked");
    }
}
