package com.mgrtech.sponti_api.contact.internal.exception;

public class CannotInviteSelfException extends RuntimeException {
    public CannotInviteSelfException() {
        super("Users cannot invite themselves");
    }
}
