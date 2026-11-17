package io.github.mighil31.registrar;

import static io.github.mighil31.registrar.Api.bearer;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base for API tests: the real app on a random port, a real MongoDB, a first admin, and helpers to
 * make members and events. Every test makes its own data, so tests don't depend on each other.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "registrar.admin.email=" + ApiIT.ADMIN_EMAIL,
            "registrar.admin.password=" + ApiIT.PASSWORD,
            "registrar.admin.name=Priya Anand"
        })
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class ApiIT {

    public static final String ADMIN_EMAIL = "admin@registrar.example";
    public static final String PASSWORD = "correct-horse-battery";

    /** A signed-in user: their id and an access token. */
    public record Account(String id, String token) {

        public String[] auth() {
            return bearer(token);
        }
    }

    @Value("${local.server.port}")
    protected int port;

    protected Api api;
    protected Account admin;

    @BeforeEach
    void signInAdmin() {
        api = new Api(port);
        admin = account(api.post("/api/v1/auth/signin", Map.of("email", ADMIN_EMAIL, "password", PASSWORD)));
    }

    protected Account member() {
        return member("Maya Okafor");
    }

    protected Account member(String name) {
        ResponseEntity<Map> response = api.post(
                "/api/v1/auth/signup",
                Map.of(
                        "name",
                        name,
                        "email",
                        "m-" + UUID.randomUUID() + "@registrar.example",
                        "phoneNumber",
                        "2125551234",
                        "password",
                        PASSWORD));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return account(response);
    }

    /** An open, in-person event a week from now; {@code changes} overrides any field. */
    protected Map<String, Object> event(Map<String, Object> changes) {
        Map<String, Object> body = eventRequest(changes);
        ResponseEntity<Map> response = api.post("/api/v1/events", body, admin.auth());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    protected Map<String, Object> event() {
        return event(Map.of());
    }

    protected static Map<String, Object> eventRequest(Map<String, Object> changes) {
        Instant start = Instant.now().plus(Duration.ofDays(7));
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Event " + UUID.randomUUID());
        body.put("description", "Bring a laptop.");
        body.put("format", "IN_PERSON");
        body.put("location", "Room 4.02");
        body.put("startsAt", start.toString());
        body.put("endsAt", start.plus(Duration.ofHours(2)).toString());
        body.put("mode", "OPEN");
        body.put("tags", List.of("workshop"));
        body.putAll(changes);
        return body;
    }

    protected Map<String, Object> getEvent(String id, Account who) {
        return api.get("/api/v1/events/" + id, who.auth()).getBody();
    }

    protected ResponseEntity<Map> register(String eventId, Account who) {
        return api.post("/api/v1/events/" + eventId + "/registrations/me", null, who.auth());
    }

    protected static String type(ResponseEntity<Map> response) {
        return (String) response.getBody().get("type");
    }

    protected static String id(Map<String, Object> body) {
        return (String) body.get("id");
    }

    private static Account account(ResponseEntity<Map> response) {
        Map<String, Object> body = response.getBody();
        return new Account(
                (String) ((Map<String, Object>) body.get("user")).get("id"), (String) body.get("accessToken"));
    }
}
