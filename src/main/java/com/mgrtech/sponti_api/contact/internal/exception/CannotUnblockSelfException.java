package com.mgrtech.sponti_api.contact.internal.exception;

public class CannotUnblockSelfException extends RuntimeException {
    public CannotUnblockSelfException() {
        super("Users cannot unblock themselves");
    }
}
