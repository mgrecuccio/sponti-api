package com.mgrtech.sponti_api.shared.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class StringUtils {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public static String normalizeE164PhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number must be valid E.164 number");
        }

        try {
            var parsedPhoneNumber = PHONE_NUMBER_UTIL.parse(phoneNumber.trim(), null);
            var normalizedPhoneNumber = PHONE_NUMBER_UTIL.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);

            if (!PHONE_NUMBER_UTIL.isValidNumber(parsedPhoneNumber)) {
                throw new IllegalArgumentException("Phone number must be valid E.164 number");
            }

            return normalizedPhoneNumber;
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Phone number must be valid E.164 number", e);
        }
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
