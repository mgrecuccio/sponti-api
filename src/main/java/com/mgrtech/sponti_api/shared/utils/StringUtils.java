package com.mgrtech.sponti_api.shared.utils;

public class StringUtils {

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
