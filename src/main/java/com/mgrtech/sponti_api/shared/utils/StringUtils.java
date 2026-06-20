package com.mgrtech.sponti_api.shared.utils;

public class StringUtils {

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
