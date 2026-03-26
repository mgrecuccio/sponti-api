package com.mgrtech.sponti_api.contact.internal.exception;

public class CannotBlockSelfException extends RuntimeException {
    public CannotBlockSelfException() {
        super("Users cannot block themselves");
    }
}
