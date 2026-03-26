package com.mgrtech.sponti_api.contact.internal.exception;

public class CannotRemoveSelfException extends RuntimeException {
    public CannotRemoveSelfException() {
        super("Users cannot remove themselves");
    }
}
