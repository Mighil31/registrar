package io.github.mighil31.registrar.auth;

import static io.github.mighil31.registrar.Api.bearer;
import static io.github.mighil31.registrar.Api.claims;
import static io.github.mighil31.registrar.Api.refreshCookie;
import static io.github.mighil31.registrar.Api.refreshToken;
import static io.github.mighil31.registrar.Api.withCookie;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.Api;
import io.github.mighil31.registrar.TestcontainersConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** Sign up, sign in, refresh, sign out and the profile, over real HTTP. No reuse grace here. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "registrar.jwt.reuse-grace=0s")
@SuppressWarnings({"rawtypes", "unchecked"})
class AuthFlowIT {

    static final String PASSWORD = "correct-horse-battery";

    @Value("${local.server.port}")
    int port;

    @Autowired
    JwtEncoder jwtEncoder;

    Api api;

    @BeforeEach
    void client() {
        api = new Api(port);
    }

    @Test
    void signUpStartsASessionAsAMember() {
        String email = uniqueEmail();
        ResponseEntity<Map> response = signUp(email);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> user = (Map<String, Object>) response.getBody().get("user");
        assertThat(user).containsEntry("email", email).containsEntry("roles", List.of("MEMBER"));
        assertThat(user).doesNotContainKey("passwordHash");
        assertThat(response.getBody().get("expiresIn")).isEqualTo(600);
        assertThat(refreshCookie(response).orElseThrow())
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth");
    }

    @Test
    void accessTokenCarriesMemberPermissionsOnly() {
        String token = accessToken(signUp(uniqueEmail()));
        Map<String, Object> claims = claims(token);

        assertThat(claims).containsEntry("iss", "registrar").containsEntry("roles", List.of("MEMBER"));
        assertThat((List<String>) claims.get("perms"))
                .containsExactly("events:browse", "profile:self", "registrations:self");
    }

    @Test
    void emailIsTakenWhateverItsCase() {
        String email = uniqueEmail();
        signUp(email);

        ResponseEntity<Map> again = signUp(email.toUpperCase());

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).containsEntry("type", "/problems/email-taken");
        assertThat((Map<String, String>) again.getBody().get("errors")).containsKey("email");
    }

    @Test
    void signUpReportsEachInvalidField() {
        ResponseEntity<Map> response = api.post(
                "/api/v1/auth/signup",
                Map.of("name", "R2D2", "email", "jane@gmail", "phoneNumber", "12ab", "password", "short"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/problem+json");
        assertThat((Map<String, String>) response.getBody().get("errors"))
                .containsOnlyKeys("name", "email", "phoneNumber", "password");
        assertThat(response.getBody()).containsKey("requestId");
    }

    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() {
        String email = uniqueEmail();
        signUp(email);

        ResponseEntity<Map> wrongPassword = signIn(email, "not-the-password");
        ResponseEntity<Map> unknownEmail = signIn(uniqueEmail(), PASSWORD);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody().get("detail"))
                .isEqualTo(unknownEmail.getBody().get("detail"));
        assertThat(signIn(email.toUpperCase(), PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void apiNeedsAValidToken() {
        ResponseEntity<Map> none = api.get("/api/v1/me");
        ResponseEntity<Map> garbage = api.get("/api/v1/me", bearer("not.a.jwt"));

        assertThat(none.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(none.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(none.getBody()).containsEntry("type", "/problems/unauthenticated");
        assertThat(garbage.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenWithoutThePermissionIsForbidden() {
        String id =
                (String) ((Map<String, Object>) signUp(uniqueEmail()).getBody().get("user")).get("id");
        Instant now = Instant.now();
        String noPermissions = jwtEncoder
                .encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                        .issuer("registrar")
                        .subject(id)
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(60))
                        .claim("perms", List.of())
                        .build()))
                .getTokenValue();

        ResponseEntity<Map> response = api.get("/api/v1/me", bearer(noPermissions));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("type", "/problems/forbidden");
    }

    @Test
    void refreshRotatesAndReuseRevokesTheWholeFamily() {
        ResponseEntity<Map> signedUp = signUp(uniqueEmail());
        String first = refreshToken(signedUp);

        ResponseEntity<Map> refreshed = refresh(first);
        String second = refreshToken(refreshed);

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second).isNotEqualTo(first);
        assertThat(accessToken(refreshed)).isNotBlank();

        ResponseEntity<Map> reused = refresh(first);
        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reused.getBody()).containsEntry("type", "/problems/session-expired");
        assertThat(refreshCookie(reused).orElseThrow()).contains("Max-Age=0");
        assertThat(refresh(second).getStatusCode())
                .as("the successor dies with its family")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshNeedsTheClientHeaderAndACookie() {
        String token = refreshToken(signUp(uniqueEmail()));

        ResponseEntity<Map> noHeader =
                api.post("/api/v1/auth/refresh", null, HttpHeaders.COOKIE, "registrar_refresh=" + token);
        ResponseEntity<Map> noCookie = api.post("/api/v1/auth/refresh", null, Api.CLIENT_HEADER, "XMLHttpRequest");

        assertThat(noHeader.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(noCookie.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(token).getStatusCode()).as("still unused").isEqualTo(HttpStatus.OK);
    }

    @Test
    void signOutEndsTheSession() {
        String token = refreshToken(signUp(uniqueEmail()));

        ResponseEntity<Map> signedOut = api.post("/api/v1/auth/signout", null, withCookie(token));

        assertThat(signedOut.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(refreshCookie(signedOut).orElseThrow()).contains("Max-Age=0");
        assertThat(refresh(token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.post("/api/v1/auth/signout", null, Api.CLIENT_HEADER, "x")
                        .getStatusCode())
                .as("no cookie: nothing to do")
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void profileCanBeReadAndChanged() {
        String token = accessToken(signUp(uniqueEmail()));

        ResponseEntity<Map> updated = api.patch("/api/v1/me", Map.of("name", "Jane O'Brien"), bearer(token));
        ResponseEntity<Map> invalid = api.patch("/api/v1/me", Map.of("phoneNumber", "123"), bearer(token));

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("name", "Jane O'Brien").containsEntry("phoneNumber", "2125551234");
        assertThat(api.get("/api/v1/me", bearer(token)).getBody()).containsEntry("name", "Jane O'Brien");
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) invalid.getBody().get("errors")).containsOnlyKeys("phoneNumber");
    }

    @Test
    void passwordChangeEndsOtherSessionsButKeepsThisOne() {
        String email = uniqueEmail();
        ResponseEntity<Map> signedUp = signUp(email);
        String otherDevice = refreshToken(signIn(email, PASSWORD));
        String token = accessToken(signedUp);

        ResponseEntity<Map> wrong = api.put(
                "/api/v1/me/password",
                Map.of("currentPassword", "nope", "newPassword", "a-whole-new-password"),
                bearer(token));
        ResponseEntity<Map> changed = api.put(
                "/api/v1/me/password",
                Map.of("currentPassword", PASSWORD, "newPassword", "a-whole-new-password"),
                bearer(token));

        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) wrong.getBody().get("errors")).containsOnlyKeys("currentPassword");
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(refresh(otherDevice).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(refreshToken(changed)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signIn(email, PASSWORD).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(signIn(email, "a-whole-new-password").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void responsesCarryARequestIdAndSecurityHeaders() {
        ResponseEntity<Map> response = api.get("/api/v1/me", "X-Request-Id", "trace-123");

        assertThat(response.getHeaders().getFirst("X-Request-Id")).isEqualTo("trace-123");
        assertThat(response.getBody()).containsEntry("requestId", "trace-123");
        assertThat(response.getHeaders().getFirst("Content-Security-Policy")).contains("default-src 'self'");
        assertThat(api.get("/api/v1/me", "X-Request-Id", "has spaces " + "x".repeat(80))
                        .getHeaders()
                        .getFirst("X-Request-Id"))
                .hasSize(32);
    }

    private ResponseEntity<Map> signUp(String email) {
        return api.post(
                "/api/v1/auth/signup",
                Map.of("name", "Jane Doe", "email", email, "phoneNumber", "2125551234", "password", PASSWORD));
    }

    private ResponseEntity<Map> signIn(String email, String password) {
        return api.post("/api/v1/auth/signin", Map.of("email", email, "password", password));
    }

    private ResponseEntity<Map> refresh(String token) {
        return api.post("/api/v1/auth/refresh", null, withCookie(token));
    }

    private static String accessToken(ResponseEntity<Map> response) {
        return (String) response.getBody().get("accessToken");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
