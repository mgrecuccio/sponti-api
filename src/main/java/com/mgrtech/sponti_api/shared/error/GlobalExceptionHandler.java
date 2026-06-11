package com.mgrtech.sponti_api.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static com.mgrtech.sponti_api.shared.error.ApiErrors.problem;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.CONFLICT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ApiErrorCode.EMAIL_ALREADY_USED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.UNAUTHORIZED.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, ApiErrorCode.BAD_CREDENTIALS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.UNAUTHORIZED.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, ApiErrorCode.INVALID_REFRESH_TOKEN, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.NOT_FOUND.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ApiErrorCode.USER_NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.NOT_FOUND.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, "Resource not found", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationError(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.BAD_REQUEST.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, "Invalid request", request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.BAD_REQUEST.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ApiErrorCode.MALFORMED_JSON, "Invalid request", request.getRequestURI());
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.BAD_REQUEST.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_REQUEST, "Invalid request", request.getRequestURI());
    }

    @ExceptionHandler(UnsupportedAuthenticationException.class)
    ProblemDetail handleUnsupportedAuthentication(Exception ex, HttpServletRequest request) {
        log.error("Request failed: status={} method={} path={}",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.UNSUPPORTED_AUTHENTICATION, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: status={} method={} path={}",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_SERVER_ERROR, "Internal server error", request.getRequestURI());
    }
}
