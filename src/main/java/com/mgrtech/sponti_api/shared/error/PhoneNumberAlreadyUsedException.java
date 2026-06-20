package com.mgrtech.sponti_api.shared.error;

public class PhoneNumberAlreadyUsedException extends RuntimeException {
    public PhoneNumberAlreadyUsedException(String message)  {
        super(message);
    }
}
