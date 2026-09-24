package com.mgrtech.sponti_api.shared.error;

public class PhoneVerificationException extends RuntimeException  {
    public PhoneVerificationException(String message) {
        super(message);
    }
}
