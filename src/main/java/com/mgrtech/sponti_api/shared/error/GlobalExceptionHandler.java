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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.CONFLICT.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            InvalidRefreshTokenException.class
    })
    ProblemDetail handleUnauthorized(RuntimeException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.UNAUTHORIZED.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.NOT_FOUND.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Request failed: status={} method={} path={} error={}",
                HttpStatus.BAD_REQUEST.value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", request.getRequestURI());
    }

    @ExceptionHandler(UnsupportedAuthenticationException.class)
    ProblemDetail handleUnsupportedAuthentication(Exception ex, HttpServletRequest request) {
        log.error("Request failed: status={} method={} path={}",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: status={} method={} path={}",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request.getRequestURI());
    }

    private ProblemDetail problem(HttpStatus status, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("path", path);
        return problem;
    }
}
