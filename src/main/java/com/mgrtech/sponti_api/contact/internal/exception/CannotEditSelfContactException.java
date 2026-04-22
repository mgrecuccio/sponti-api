package com.mgrtech.sponti_api.contact.internal.exception;

public class CannotEditSelfContactException extends RuntimeException {
    public CannotEditSelfContactException() {
        super("Users cannot edit their own contact");
    }
}