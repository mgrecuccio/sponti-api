package com.mgrtech.sponti_api.shared.utils;

import jakarta.servlet.http.HttpServletRequest;

public class HttpRequestUtils {

    private HttpRequestUtils() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
