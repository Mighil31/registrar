package io.github.mighil31.registrar.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.ApiIT;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuppressWarnings({"rawtypes", "unchecked"})
class EventIT extends ApiIT {

    @Test
    void membersBrowseOnlyOpenScheduledUpcomingEvents() {
        String batch = "Batch " + UUID.randomUUID();
        Instant past = Instant.now().minus(Duration.ofDays(2));
        event(Map.of("title", batch + " open"));
        event(Map.of("title", batch + " private", "mode", "PRIVATE"));
        event(Map.of(
                "title", batch + " past",
                "startsAt", past.toString(),
                "endsAt", past.plusSeconds(3600).toString()));
        String cancelled = id(event(Map.of("title", batch + " cancelled")));
        api.post("/api/v1/events/" + cancelled + "/cancel", null, admin.auth());

        assertThat(titles(api.get("/api/v1/events?q=" + batch, member().auth())))
                .containsExactly(batch + " open");
        assertThat(titles(api.get("/api/v1/events?scope=ALL&q=" + batch, admin.auth())))
                .hasSize(4);
        assertThat(titles(api.get("/api/v1/events?scope=ALL&mode=PRIVATE&q=" + batch, admin.auth())))
                .containsExactly(batch + " private");
    }

    @Test
    void browseFiltersByTagAndPages() {
        String batch = "Tagged " + UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            event(Map.of("title", batch + " " + i, "tags", List.of("social")));
        }
        event(Map.of("title", batch + " other", "tags", List.of("talk")));

        ResponseEntity<Map> page = api.get("/api/v1/events?tag=social&size=2&q=" + batch, member().auth());

        assertThat(titles(page)).hasSize(2);
        assertThat(page.getBody()).containsEntry("total", 3).containsEntry("size", 2);
    }

    @Test
    void onlyAdminsManageEvents() {
        Account member = member();

        assertThat(api.post("/api/v1/events", eventRequest(Map.of()), member.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.get("/api/v1/events?scope=ALL", member.auth()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.delete("/api/v1/events/" + id(event()), member.auth()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createReportsEachInvalidField() {
        Instant start = Instant.now().plus(Duration.ofDays(1));
        ResponseEntity<Map> response = api.post(
                "/api/v1/events",
                eventRequest(Map.of(
                        "title", " ",
                        "endsAt", start.minusSeconds(60).toString(),
                        "startsAt", start.toString(),
                        "tags", List.of("Not A Tag"),
                        "capacity", 0)),
                admin.auth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) response.getBody().get("errors")).containsKeys("title", "tags[0]", "capacity");

        ResponseEntity<Map> backwards = api.post(
                "/api/v1/events",
                eventRequest(Map.of("startsAt", start.toString(), "endsAt", start.toString())),
                admin.auth());
        assertThat((Map<String, String>) backwards.getBody().get("errors")).containsOnlyKeys("endsAt");
    }

    @Test
    void privateEventsAreHiddenFromMembersUntilTheyAreAdded() {
        String id = id(event(Map.of("mode", "PRIVATE", "format", "ONLINE", "meetingLink", "https://meet.example/abc")));
        Account member = member();

        assertThat(api.get("/api/v1/events/" + id, member.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        api.post("/api/v1/events/" + id + "/registrations", Map.of("userIds", List.of(member.id())), admin.auth());

        assertThat(getEvent(id, member))
                .containsEntry("registered", true)
                .containsEntry("meetingLink", "https://meet.example/abc");
    }

    @Test
    void meetingLinkIsOnlyForAttendeesAndAdmins() {
        String id = id(event(Map.of("format", "ONLINE", "meetingLink", "https://meet.example/xyz")));

        assertThat(getEvent(id, member())).containsEntry("meetingLink", null);
        assertThat(getEvent(id, admin)).containsEntry("meetingLink", "https://meet.example/xyz");
    }

    @Test
    void anEditBasedOnAStaleVersionIsAConflict() {
        Map<String, Object> created = event();
        String id = id(created);

        ResponseEntity<Map> first = api.put(
                "/api/v1/events/" + id, eventRequest(Map.of("title", "First edit", "version", 0)), admin.auth());
        ResponseEntity<Map> second = api.put(
                "/api/v1/events/" + id, eventRequest(Map.of("title", "Second edit", "version", 0)), admin.auth());
        ResponseEntity<Map> noVersion =
                api.put("/api/v1/events/" + id, eventRequest(Map.of("title", "No version")), admin.auth());

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).containsEntry("version", 1).containsEntry("title", "First edit");
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(type(second)).isEqualTo("/problems/conflict");
        assertThat((Map<String, String>) noVersion.getBody().get("errors")).containsOnlyKeys("version");
        assertThat(getEvent(id, admin)).containsEntry("title", "First edit");
    }

    @Test
    void registrationsDontMakeAnEditStale() {
        String id = id(event());
        register(id, member());

        ResponseEntity<Map> edit =
                api.put("/api/v1/events/" + id, eventRequest(Map.of("title", "Renamed", "version", 0)), admin.auth());

        assertThat(edit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edit.getBody()).containsEntry("registeredCount", 1);
    }

    @Test
    void editsAndCancellationReachTheCopiesInMyEvents() {
        String id = id(event());
        Account member = member();
        register(id, member);

        api.put("/api/v1/events/" + id, eventRequest(Map.of("title", "Moved and renamed", "version", 0)), admin.auth());
        api.post("/api/v1/events/" + id + "/cancel", null, admin.auth());

        assertThat(myEvents(member))
                .singleElement()
                .satisfies(r -> assertThat(r)
                        .containsEntry("title", "Moved and renamed")
                        .containsEntry("status", "CANCELLED"));

        api.post("/api/v1/events/" + id + "/uncancel", null, admin.auth());
        assertThat(myEvents(member))
                .singleElement()
                .satisfies(r -> assertThat(r).containsEntry("status", "SCHEDULED"));
    }

    @Test
    void deletingAnEventDeletesItsRegistrations() {
        String id = id(event());
        Account member = member();
        register(id, member);

        assertThat(api.delete("/api/v1/events/" + id, admin.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(myEvents(member)).isEmpty();
        assertThat(api.get("/api/v1/events/" + id, admin.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.delete("/api/v1/events/" + id, admin.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownOrMalformedIdsAreNotFound() {
        assertThat(api.get("/api/v1/events/not-an-id", admin.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.get("/api/v1/events/0123456789abcdef01234567", admin.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.post("/api/v1/events/0123456789abcdef01234567/cancel", null, admin.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private List<Map<String, Object>> myEvents(Account who) {
        return api.list("/api/v1/me/registrations", who.auth()).getBody();
    }

    private static List<String> titles(ResponseEntity<Map> page) {
        return ((List<Map<String, Object>>) page.getBody().get("items"))
                .stream().map(e -> (String) e.get("title")).toList();
    }
}
