package com.mgrtech.sponti_api.shared.error;

public class UnexpectedPhoneNumberException extends RuntimeException {
    public UnexpectedPhoneNumberException(String message) {
        super(message);
    }
}
