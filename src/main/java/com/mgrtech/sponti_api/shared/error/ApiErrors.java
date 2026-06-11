package com.mgrtech.sponti_api.shared.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ApiErrors {

    private ApiErrors() {
    }

    public static ProblemDetail problem(HttpStatus status, ApiErrorCode code, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code.value());
        problem.setProperty("path", path);
        return problem;
    }

    public static String problemJson(HttpStatus status, ApiErrorCode code, String detail, String path) {
        return """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","code":"%s","path":"%s"}
                """.formatted(
                escape(status.getReasonPhrase()),
                status.value(),
                escape(detail),
                escape(code.value()),
                escape(path)
        );
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
