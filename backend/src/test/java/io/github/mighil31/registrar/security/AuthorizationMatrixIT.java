package io.github.mighil31.registrar.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import io.github.mighil31.registrar.Api;
import io.github.mighil31.registrar.ApiIT;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Every endpoint × {anonymous, member, admin}:
 *
 * <pre>
 *                 anonymous   member   admin
 *   public           2xx        2xx     2xx
 *   signed in        401        2xx     2xx
 *   admin only       401        403     2xx
 * </pre>
 *
 * <p>Each call is a valid request against real records, made fresh for that call. That matters:
 * Spring binds the path and validates the body before {@code @PreAuthorize} runs, so a sloppy
 * request would get a 400 and prove nothing about permissions. A second test checks the table
 * against the OpenAPI document, so a new endpoint fails the build until it has a row here.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class AuthorizationMatrixIT extends ApiIT {

    enum Who {
        ANONYMOUS,
        MEMBER,
        ADMIN
    }

    enum Needs {
        NOTHING,
        SIGN_IN,
        ADMIN
    }

    /** One call's world: who is calling, another person to act on, and an open event. */
    record Fixture(Account caller, Account other, Map<String, Object> event) {

        String[] auth() {
            return caller == null ? new String[0] : caller.auth();
        }

        String eventId() {
            return (String) event.get("id");
        }
    }

    /** The body and any extra headers; may also set things up first (register, cancel, ...). */
    record Request(Object body, String... headers) {}

    record Endpoint(HttpMethod method, String path, Needs needs, Function<Fixture, Request> prepare) {

        String key() {
            return method.name() + " " + path;
        }
    }

    private static Request none() {
        return new Request(null);
    }

    private List<Endpoint> endpoints() {
        String auth = "/api/v1/auth";
        String events = "/api/v1/events";
        String users = "/api/v1/users";
        return List.of(
                // Public
                new Endpoint(POST, auth + "/signup", Needs.NOTHING, f -> new Request(signUpBody())),
                new Endpoint(
                        POST,
                        auth + "/signin",
                        Needs.NOTHING,
                        f -> new Request(Map.of("email", f.other().email(), "password", PASSWORD))),
                new Endpoint(POST, auth + "/refresh", Needs.NOTHING, f -> new Request(null, freshRefreshCookie())),
                new Endpoint(POST, auth + "/signout", Needs.NOTHING, f -> new Request(null, freshRefreshCookie())),
                // Any signed-in user
                new Endpoint(GET, "/api/v1/me", Needs.SIGN_IN, f -> none()),
                new Endpoint(PATCH, "/api/v1/me", Needs.SIGN_IN, f -> new Request(Map.of("name", "Maya Okafor"))),
                new Endpoint(
                        PUT,
                        "/api/v1/me/password",
                        Needs.SIGN_IN,
                        f -> new Request(Map.of("currentPassword", PASSWORD, "newPassword", "a-new-long-password"))),
                new Endpoint(GET, "/api/v1/me/registrations", Needs.SIGN_IN, f -> none()),
                new Endpoint(GET, events, Needs.SIGN_IN, f -> none()),
                new Endpoint(GET, events + "/{id}", Needs.SIGN_IN, f -> none()),
                new Endpoint(POST, events + "/{id}/registrations/me", Needs.SIGN_IN, f -> none()),
                new Endpoint(DELETE, events + "/{id}/registrations/me", Needs.SIGN_IN, f -> {
                    if (f.caller() != null) {
                        register(f.eventId(), f.caller());
                    }
                    return none();
                }),
                // Admins only
                new Endpoint(GET, "/api/v1/dashboard", Needs.ADMIN, f -> none()),
                new Endpoint(GET, users, Needs.ADMIN, f -> none()),
                new Endpoint(GET, users + "/{id}", Needs.ADMIN, f -> none()),
                new Endpoint(GET, users + "/{id}/registrations", Needs.ADMIN, f -> none()),
                new Endpoint(
                        PUT,
                        users + "/{id}/roles",
                        Needs.ADMIN,
                        f -> new Request(Map.of("roles", List.of("MEMBER", "ADMIN")))),
                new Endpoint(DELETE, users + "/{id}", Needs.ADMIN, f -> none()),
                new Endpoint(POST, events, Needs.ADMIN, f -> new Request(eventRequest(Map.of()))),
                new Endpoint(
                        PUT,
                        events + "/{id}",
                        Needs.ADMIN,
                        f -> new Request(eventRequest(
                                Map.of("title", "Renamed", "version", f.event().get("version"))))),
                new Endpoint(POST, events + "/{id}/cancel", Needs.ADMIN, f -> none()),
                new Endpoint(POST, events + "/{id}/uncancel", Needs.ADMIN, f -> {
                    api.post(events + "/" + f.eventId() + "/cancel", null, admin.auth());
                    return none();
                }),
                new Endpoint(DELETE, events + "/{id}", Needs.ADMIN, f -> none()),
                new Endpoint(GET, events + "/{id}/registrations", Needs.ADMIN, f -> none()),
                new Endpoint(
                        POST,
                        events + "/{id}/registrations",
                        Needs.ADMIN,
                        f -> new Request(Map.of("userIds", List.of(f.other().id())))),
                new Endpoint(DELETE, events + "/{id}/registrations/{userId}", Needs.ADMIN, f -> {
                    addToEvent(f.eventId(), f.other());
                    return none();
                }));
    }

    @TestFactory
    Stream<DynamicTest> everyEndpointAnswersEachKindOfCallerCorrectly() {
        return endpoints().stream()
                .flatMap(endpoint -> Stream.of(Who.values())
                        .map(who -> dynamicTest(
                                endpoint.key() + " as " + who + " → " + expected(endpoint.needs(), who),
                                () -> check(endpoint, who))));
    }

    @Test
    void theMatrixCoversEveryOperationInTheApi() {
        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>)
                api.get("/v3/api-docs").getBody().get("paths");
        Set<String> verbs = Set.of("get", "put", "post", "patch", "delete");
        Set<String> documented = paths.entrySet().stream()
                .flatMap(path -> path.getValue().keySet().stream()
                        .filter(verbs::contains)
                        .map(verb -> verb.toUpperCase() + " " + path.getKey()))
                .collect(Collectors.toSet());

        assertThat(endpoints().stream().map(Endpoint::key)).containsExactlyInAnyOrderElementsOf(documented);
    }

    private static String expected(Needs needs, Who who) {
        if (needs == Needs.NOTHING || who == Who.ADMIN || (needs == Needs.SIGN_IN && who == Who.MEMBER)) {
            return "2xx";
        }
        return who == Who.ANONYMOUS ? "401" : "403";
    }

    private void check(Endpoint endpoint, Who who) {
        Fixture fixture = new Fixture(caller(who), member("Tom Reyes"), event(Map.of("capacity", 50)));
        Request request = endpoint.prepare().apply(fixture);
        String path = endpoint.path()
                .replace("{userId}", fixture.other().id())
                .replace(
                        "{id}",
                        endpoint.path().startsWith("/api/v1/users")
                                ? fixture.other().id()
                                : fixture.eventId());
        List<String> headers = new ArrayList<>(List.of(fixture.auth()));
        headers.addAll(List.of(request.headers()));

        ResponseEntity<String> response =
                api.send(endpoint.method(), path, request.body(), headers.toArray(String[]::new));

        String want = expected(endpoint.needs(), who);
        String description = endpoint.method() + " " + path + " → " + response.getBody();
        if (want.equals("2xx")) {
            assertThat(response.getStatusCode().is2xxSuccessful())
                    .as(description)
                    .isTrue();
        } else {
            assertThat(response.getStatusCode().value()).as(description).isEqualTo(Integer.parseInt(want));
            assertThat(response.getHeaders().getContentType())
                    .as(description)
                    .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        }
    }

    private Account caller(Who who) {
        return switch (who) {
            case ANONYMOUS -> null;
            case MEMBER -> member("Maya Okafor");
            case ADMIN -> newAdmin("Ada Lovelace");
        };
    }

    private void addToEvent(String eventId, Account person) {
        api.post("/api/v1/events/" + eventId + "/registrations", Map.of("userIds", List.of(person.id())), admin.auth());
    }

    private static Map<String, Object> signUpBody() {
        return Map.of(
                "name",
                "Sam Ortiz",
                "email",
                "s-" + UUID.randomUUID() + "@registrar.example",
                "phoneNumber",
                "2125551234",
                "password",
                PASSWORD);
    }

    /** A new account's refresh cookie, with the client header the cookie endpoints require. */
    private String[] freshRefreshCookie() {
        ResponseEntity<Map> signUp = api.post("/api/v1/auth/signup", signUpBody());
        return Api.withCookie(Api.refreshToken(signUp));
    }
}
