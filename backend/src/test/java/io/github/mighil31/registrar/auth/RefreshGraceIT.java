package io.github.mighil31.registrar.auth;

import static io.github.mighil31.registrar.Api.refreshToken;
import static io.github.mighil31.registrar.Api.withCookie;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.Api;
import io.github.mighil31.registrar.TestcontainersConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

/**
 * Two tabs refreshing at once present the same token twice. Within the grace window the late one
 * is refused, but the session survives: no family revocation.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RefreshGraceIT {

    @Value("${local.server.port}")
    int port;

    @Test
    void secondUseWithinGraceIsRefusedWithoutEndingTheSession() {
        Api api = new Api(port);
        String first = refreshToken(api.post(
                "/api/v1/auth/signup",
                Map.of(
                        "name", "Tab Tester",
                        "email", "tabs@example.com",
                        "phoneNumber", "2125551234",
                        "password", "correct-horse-battery")));

        String second = refreshToken(api.post("/api/v1/auth/refresh", null, withCookie(first)));

        assertThat(api.post("/api/v1/auth/refresh", null, withCookie(first)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.post("/api/v1/auth/refresh", null, withCookie(second)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
