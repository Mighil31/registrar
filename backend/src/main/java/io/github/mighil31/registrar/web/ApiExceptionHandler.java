package io.github.mighil31.registrar.web;

import io.github.mighil31.registrar.auth.EmailTakenException;
import io.github.mighil31.registrar.auth.InvalidCredentialsException;
import io.github.mighil31.registrar.auth.InvalidRefreshTokenException;
import io.github.mighil31.registrar.auth.MissingClientHeaderException;
import io.github.mighil31.registrar.auth.RefreshCookie;
import io.github.mighil31.registrar.me.WrongPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every API error as ProblemDetail. Spring MVC's own errors (unreadable body, wrong method) get the
 * base class's generic text, so no parser or framework internals reach the client.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** 400 with one message per invalid field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail body = Problems.of(HttpStatus.BAD_REQUEST, "validation-error", "Validation failed", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler
    ProblemDetail problem(ProblemException ex) {
        return ex.errors() == null
                ? Problems.of(ex.status(), ex.type(), ex.getMessage())
                : Problems.of(ex.status(), ex.type(), ex.getMessage(), ex.errors());
    }

    @ExceptionHandler
    ProblemDetail emailTaken(EmailTakenException ex) {
        return Problems.of(
                HttpStatus.CONFLICT, "email-taken", ex.getMessage(), Map.of("email", "is already registered"));
    }

    @ExceptionHandler
    ProblemDetail invalidCredentials(InvalidCredentialsException ex) {
        return Problems.of(HttpStatus.UNAUTHORIZED, "invalid-credentials", ex.getMessage());
    }

    /** Also tells the browser to drop the dead cookie. */
    @ExceptionHandler
    ResponseEntity<ProblemDetail> invalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.clear())
                .body(Problems.of(HttpStatus.UNAUTHORIZED, "session-expired", ex.getMessage()));
    }

    @ExceptionHandler
    ProblemDetail missingClientHeader(MissingClientHeaderException ex) {
        return Problems.of(HttpStatus.FORBIDDEN, "forbidden", ex.getMessage());
    }

    @ExceptionHandler
    ProblemDetail wrongPassword(WrongPasswordException ex) {
        return Problems.of(
                HttpStatus.BAD_REQUEST, "validation-error", "Validation failed", Map.of("currentPassword", "is wrong"));
    }

    @ExceptionHandler
    ProblemDetail staleVersion(OptimisticLockingFailureException ex) {
        return Problems.of(HttpStatus.CONFLICT, "conflict", "Changed by someone else since you loaded it; reload");
    }

    /** {@code @PreAuthorize} failures reach MVC, not the security filters. */
    @ExceptionHandler
    ProblemDetail accessDenied(AccessDeniedException ex) {
        return Problems.of(HttpStatus.FORBIDDEN, "forbidden", "Access denied");
    }

    /** Generic message plus the request id, which finds the full exception in the log. */
    @ExceptionHandler
    ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected", "Unexpected error");
    }
}
