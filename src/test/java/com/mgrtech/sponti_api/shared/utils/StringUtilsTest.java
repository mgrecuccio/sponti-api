package com.mgrtech.sponti_api.shared.utils;

import org.junit.jupiter.api.Test;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.normalizeE164PhoneNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringUtilsTest {

    @Test
    void normalizes_valid_E164_phone_number() {
        assertThat(normalizeE164PhoneNumber(" +32468009911 "))
                .isEqualTo("+32468009911");
    }

    @Test
    void rejects_invalid_phone_number() {
        assertThatThrownBy(() -> normalizeE164PhoneNumber("+999123456789"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number must be valid E.164 number");
    }

    @Test
    void rejects_blank_phone_number() {
        assertThatThrownBy(() -> normalizeE164PhoneNumber(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number must be valid E.164 number");
    }

    @Test
    void rejects_null_phone_number() {
        assertThatThrownBy(() -> normalizeE164PhoneNumber(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone number must be valid E.164 number");
    }
}
