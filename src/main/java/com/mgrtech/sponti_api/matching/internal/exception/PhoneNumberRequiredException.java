package com.mgrtech.sponti_api.matching.internal.exception;

public class PhoneNumberRequiredException extends RuntimeException {
    public PhoneNumberRequiredException(String message){
        super(message);
    }
}
