package io.github.mighil31.registrar.web;

import java.net.URI;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * RFC 9457 error bodies. {@code type} is a stable identifier a client can switch on
 * ({@code /problems/email-taken}); {@code requestId} finds the request's log lines.
 */
public final class Problems {

    private Problems() {}

    public static ProblemDetail of(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("/problems/" + type));
        problem.setProperty("requestId", MDC.get(RequestIdFilter.MDC_KEY));
        return problem;
    }

    public static ProblemDetail of(HttpStatus status, String type, String detail, Map<String, String> errors) {
        ProblemDetail problem = of(status, type, detail);
        problem.setProperty("errors", errors);
        return problem;
    }
}
