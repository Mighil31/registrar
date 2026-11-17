package io.github.mighil31.registrar.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign up, sign in, refresh, sign out. Refresh and sign-out authenticate by cookie, so they also
 * require {@code X-Requested-With}: a cross-site form can't set it (SameSite=Strict already stops
 * the cookie; this is the second lock).
 */
@RestController
@RequestMapping(RefreshCookie.PATH)
class AuthController {

    static final String CLIENT_HEADER = "X-Requested-With";

    private final AuthService auth;
    private final RefreshCookie cookie;

    AuthController(AuthService auth, RefreshCookie cookie) {
        this.auth = auth;
        this.cookie = cookie;
    }

    @PostMapping("/signup")
    ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return respond(HttpStatus.CREATED, auth.signUp(request));
    }

    @PostMapping("/signin")
    ResponseEntity<AuthResponse> signIn(@Valid @RequestBody SignInRequest request) {
        return respond(HttpStatus.OK, auth.signIn(request));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(
            @RequestHeader(name = CLIENT_HEADER, required = false) String client,
            @CookieValue(name = RefreshCookie.NAME, required = false) String token) {
        requireClient(client);
        if (token == null || token.isEmpty()) {
            throw new InvalidRefreshTokenException();
        }
        return respond(HttpStatus.OK, auth.refresh(token));
    }

    @PostMapping("/signout")
    ResponseEntity<Void> signOut(
            @RequestHeader(name = CLIENT_HEADER, required = false) String client,
            @CookieValue(name = RefreshCookie.NAME, required = false) String token) {
        requireClient(client);
        if (token != null && !token.isEmpty()) {
            auth.signOut(token);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.clear())
                .build();
    }

    private ResponseEntity<AuthResponse> respond(HttpStatus status, AuthService.Session session) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.set(session.refreshToken()))
                .body(session.response());
    }

    private static void requireClient(String client) {
        if (client == null || client.isBlank()) {
            throw new MissingClientHeaderException();
        }
    }
}
