package com.mgrtech.sponti_api.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            InvalidRefreshTokenException.class
    })
    ProblemDetail handleUnauthorized(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", request.getRequestURI());
    }

    @ExceptionHandler(UnsupportedAuthenticationException.class)
    ProblemDetail handleUnsupportedAuthentication(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    private ProblemDetail problem(HttpStatus status, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("path", path);
        return problem;
    }
}