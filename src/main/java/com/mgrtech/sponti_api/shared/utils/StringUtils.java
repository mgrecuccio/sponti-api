package com.mgrtech.sponti_api.shared.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

public class StringUtils {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

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

    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "na";
        }

        var trimmed = phoneNumber.trim();
        if (trimmed.length() <= 4) {
            return "***";
        }

        return "***" + trimmed.substring(trimmed.length() - 4);
    }

    public static String normalizedClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }

        return clientIp.trim();
    }
}
