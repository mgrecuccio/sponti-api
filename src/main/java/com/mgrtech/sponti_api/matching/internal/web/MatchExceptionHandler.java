package com.mgrtech.sponti_api.matching.internal.web;

import com.mgrtech.sponti_api.matching.internal.exception.*;
import com.mgrtech.sponti_api.shared.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.mgrtech.sponti_api.shared.error.ApiErrors.problem;

@RestControllerAdvice(basePackages = "com.mgrtech.sponti_api.matching.internal.web")
public class MatchExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MatchExceptionHandler.class);

    @ExceptionHandler(MatchingDisabledException.class)
    ProblemDetail handleMatchingDisabled(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.UNPROCESSABLE_CONTENT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ApiErrorCode.MATCHING_DISABLED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(PhoneNumberRequiredException.class)
    ProblemDetail handlePhoneNumberRequired(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.UNPROCESSABLE_CONTENT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ApiErrorCode.PHONE_NUMBER_REQUIRED_FOR_MATCH_ACCEPTANCE, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ChannelNotAllowedException.class)
    ProblemDetail handleChannelNotAllowed(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.UNPROCESSABLE_CONTENT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ApiErrorCode.CHANNEL_NOT_ALLOWED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AvailabilityOverlapNotFoundException.class)
    ProblemDetail handleAvailabilityOverlapNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.UNPROCESSABLE_CONTENT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ApiErrorCode.AVAILABILITY_OVERLAP_NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MatchScoreBelowThresholdException.class)
    ProblemDetail handleMatchScoreBelowThreshold(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.UNPROCESSABLE_CONTENT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ApiErrorCode.MATCH_SCORE_BELOW_THRESHOLD, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MatchAlreadyExistsException.class)
    ProblemDetail handleMatchConflict(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.CONFLICT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ApiErrorCode.MATCH_ALREADY_EXISTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MatchProposalExpiredException.class)
    ProblemDetail handleMatchProposalExpired(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.CONFLICT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ApiErrorCode.MATCH_PROPOSAL_EXPIRED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AcceptedContactNotFoundException.class)
    ProblemDetail handleAcceptedContactNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.NOT_FOUND.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ApiErrorCode.ACCEPTED_CONTACT_NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MatchNotFoundException.class)
    ProblemDetail handleMatchNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.NOT_FOUND.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ApiErrorCode.MATCH_NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.CONFLICT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ApiErrorCode.MATCH_STATE_CONFLICT, ex.getMessage(), request.getRequestURI());
    }
}
