package com.mgrtech.sponti_api.availability.internal.web;

import com.mgrtech.sponti_api.availability.internal.exception.AvailabilityRuleNotFoundException;
import com.mgrtech.sponti_api.availability.internal.exception.InvalidAvailabilityOverrideTimeRangeException;
import com.mgrtech.sponti_api.availability.internal.exception.InvalidAvailabilityRuleTimeRangeException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AvailabilityController.class)
public class AvailabilityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityExceptionHandler.class);

    @ExceptionHandler(AvailabilityRuleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail handleRuleNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.NOT_FOUND.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            InvalidAvailabilityRuleTimeRangeException.class,
            InvalidAvailabilityOverrideTimeRangeException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.BAD_REQUEST.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleHttpMessageNotReadable(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.BAD_REQUEST.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    private ProblemDetail problem(HttpStatus status, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("path", path);
        return problem;
    }
}
