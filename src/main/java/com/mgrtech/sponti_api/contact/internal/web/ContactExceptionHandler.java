package com.mgrtech.sponti_api.contact.internal.web;

import com.mgrtech.sponti_api.contact.internal.exception.*;
import com.mgrtech.sponti_api.shared.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.mgrtech.sponti_api.shared.error.ApiErrors.problem;

@RestControllerAdvice(basePackages = "com.mgrtech.sponti_api.contact.internal.web")
public class ContactExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ContactExceptionHandler.class);

    @ExceptionHandler({
            CannotInviteSelfException.class,
            CannotBlockSelfException.class,
            CannotRemoveSelfException.class,
            CannotEditSelfContactException.class
    })
    ProblemDetail handleCannotInviteSelf(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.BAD_REQUEST.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ApiErrorCode.CONTACT_SELF_OPERATION_NOT_ALLOWED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ContactInvitationAlreadyExistsException.class)
    ProblemDetail handleInvitationAlreadyExists(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.CONFLICT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ApiErrorCode.CONTACT_INVITATION_ALREADY_EXISTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ContactAlreadyExistsException.class)
    ProblemDetail handleContactAlreadyExists(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.CONFLICT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ApiErrorCode.CONTACT_ALREADY_EXISTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ContactInvitationNotFoundException.class)
    ProblemDetail handleInvitationNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.NOT_FOUND.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ApiErrorCode.CONTACT_INVITATION_NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ContactNotFoundException.class)
    ProblemDetail handleContactNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.NOT_FOUND.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ApiErrorCode.CONTACT_NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ContactBlockedException.class)
    ProblemDetail handleContactBlocked(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.CONFLICT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ApiErrorCode.CONTACT_BLOCKED, ex.getMessage(), request.getRequestURI());
    }
}
