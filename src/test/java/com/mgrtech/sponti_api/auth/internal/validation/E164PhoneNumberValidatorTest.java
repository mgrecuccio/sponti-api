package com.mgrtech.sponti_api.auth.internal.validation;

import com.mgrtech.sponti_api.shared.validation.E164PhoneNumberValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class E164PhoneNumberValidatorTest {

    private final E164PhoneNumberValidator validator = new E164PhoneNumberValidator();

    @Test
    void empty_or_null_phone_numbers_are_valid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void rejects_invalid_phone_numbers() {
        var phoneNumber = "Invalid";
        assertThat(validator.isValid(phoneNumber, null)).isFalse();
    }

    @Test
    void rejects_non_E164_format_phone_numbers() {
        var nonE164PhoneNumber = "+999123456789";
        assertThat(validator.isValid(nonE164PhoneNumber, null)).isFalse();
    }

    @Test
    void accepts_E164_format_phone_numbers() {
        var e164PhoneNumber = "+32468009911";
        assertThat(validator.isValid(e164PhoneNumber, null)).isTrue();
    }

}