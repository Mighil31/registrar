package io.github.mighil31.registrar.security;

import io.github.mighil31.registrar.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 401 and 403 raised in the security filters (before Spring MVC) as ProblemDetail, like every other
 * error. A missing, malformed or expired token all read the same: the client refreshes or signs in.
 */
class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, Problems.of(HttpStatus.UNAUTHORIZED, "unauthenticated", "Sign in required"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(request, response, Problems.of(HttpStatus.FORBIDDEN, "forbidden", "Access denied"));
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ProblemDetail body)
            throws IOException {
        body.setInstance(URI.create(request.getRequestURI()));
        ServletServerHttpResponse out = new ServletServerHttpResponse(response);
        out.setStatusCode(HttpStatus.valueOf(body.getStatus()));
        converter.write(body, MediaType.APPLICATION_PROBLEM_JSON, out);
    }
}
