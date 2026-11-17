package io.github.mighil31.registrar.auth;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The refresh token's cookie: unreadable by scripts (HttpOnly), HTTPS only (browsers also accept it
 * on http://localhost), never sent cross-site (SameSite=Strict), and only to the auth endpoints.
 */
@Component
public class RefreshCookie {

    public static final String NAME = "registrar_refresh";
    static final String PATH = "/api/v1/auth";

    private final RefreshTokens refreshTokens;

    RefreshCookie(RefreshTokens refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    public String set(String token) {
        return cookie(token, Duration.ofSeconds(refreshTokens.ttlSeconds()));
    }

    /** Tells the browser to drop the cookie (sign-out, or a token that no longer works). */
    public static String clear() {
        return cookie("", Duration.ZERO);
    }

    private static String cookie(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
