package com.mgrtech.sponti_api.contact.internal.exception;

public class ContactAlreadyExistsException extends RuntimeException {

    public ContactAlreadyExistsException() {
        super("Contact relationship already exists");
    }
}
