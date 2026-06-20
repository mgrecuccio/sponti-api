package com.mgrtech.sponti_api.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = E164PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidE164PhoneNumber {
    String message() default "Phone number must be valid E.164 number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
