package io.github.mighil31.registrar.registration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.ApiIT;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuppressWarnings({"rawtypes", "unchecked"})
class RegistrationIT extends ApiIT {

    @Test
    void memberRegistersOnceAndLeaves() {
        String id = id(event());
        Account member = member();

        ResponseEntity<Map> registered = register(id, member);
        ResponseEntity<Map> again = register(id, member);

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getBody()).containsEntry("eventId", id);
        assertThat(type(again)).isEqualTo("/problems/already-registered");
        assertThat(getEvent(id, member)).containsEntry("registered", true).containsEntry("registeredCount", 1);

        assertThat(leave(id, member).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getEvent(id, member)).containsEntry("registered", false).containsEntry("registeredCount", 0);
        assertThat(leave(id, member).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aFullEventRefusesMembers() {
        String id = id(event(Map.of("capacity", 1)));
        register(id, member());

        ResponseEntity<Map> second = register(id, member());

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(type(second)).isEqualTo("/problems/event-full");
        assertThat(getEvent(id, admin)).containsEntry("registeredCount", 1);
    }

    @Test
    void membersCantJoinPrivateEvents() {
        String id = id(event(Map.of("mode", "PRIVATE")));

        assertThat(register(id, member()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelledAndStartedEventsAreClosed() {
        String cancelled = id(event());
        api.post("/api/v1/events/" + cancelled + "/cancel", null, admin.auth());
        Instant past = Instant.now().minus(Duration.ofHours(1));
        String started = id(event(Map.of(
                "startsAt", past.toString(), "endsAt", past.plusSeconds(7200).toString())));
        Account member = member();

        assertThat(register(cancelled, member).getBody()).containsEntry("detail", "This event was cancelled");
        assertThat(type(register(started, member))).isEqualTo("/problems/event-closed");

        addPeople(started, member.id());
        ResponseEntity<Map> leave = leave(started, member);
        assertThat(leave.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(type(leave)).isEqualTo("/problems/event-closed");
    }

    @Test
    void adminsCanAddPastCapacityAndThoseRowsAreMarked() {
        String id = id(event(Map.of("capacity", 1)));
        Account a = member("Ana Ruiz");
        Account b = member("Ben Ito");
        Account c = member("Cy Park");

        ResponseEntity<Map> added = addPeople(id, a.id(), b.id(), c.id());

        assertThat(added.getBody())
                .containsEntry("added", List.of(a.id(), b.id(), c.id()))
                .containsEntry("registeredCount", 3)
                .containsEntry("capacity", 1);
        assertThat(attendees(id))
                .extracting(r -> r.get("name"), r -> r.get("overCapacity"), r -> r.get("addedByAdmin"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Ana Ruiz", false, true),
                        org.assertj.core.groups.Tuple.tuple("Ben Ito", true, true),
                        org.assertj.core.groups.Tuple.tuple("Cy Park", true, true));

        ResponseEntity<Map> again = addPeople(id, a.id());
        assertThat(again.getBody())
                .containsEntry("added", List.of())
                .containsEntry("alreadyRegistered", List.of(a.id()))
                .containsEntry("registeredCount", 3);
    }

    @Test
    void addingAnUnknownUserChangesNothing() {
        String id = id(event());
        Account member = member();

        ResponseEntity<Map> response = addPeople(id, member.id(), "0123456789abcdef01234567");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) response.getBody().get("errors")).containsOnlyKeys("userIds");
        assertThat(attendees(id)).isEmpty();
    }

    @Test
    void adminsRemovePeopleAtAnyTime() {
        Instant past = Instant.now().minus(Duration.ofHours(1));
        String id = id(event(Map.of(
                "startsAt", past.toString(), "endsAt", past.plusSeconds(7200).toString())));
        Account member = member();
        addPeople(id, member.id());

        assertThat(api.delete("/api/v1/events/" + id + "/registrations/" + member.id(), admin.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(getEvent(id, admin)).containsEntry("registeredCount", 0);
        assertThat(api.delete("/api/v1/events/" + id + "/registrations/" + member.id(), admin.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void membersSeeCountsNotNames() {
        String id = id(event());
        Account member = member();

        assertThat(api.get("/api/v1/events/" + id + "/registrations", member.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.post(
                                "/api/v1/events/" + id + "/registrations",
                                Map.of("userIds", List.of(member.id())),
                                member.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void myEventsSplitsUpcomingAndPast() {
        Instant past = Instant.now().minus(Duration.ofDays(3));
        String soon = id(event(Map.of("title", "Soon")));
        String later = id(event(Map.of(
                "title", "Later",
                "startsAt", Instant.now().plus(Duration.ofDays(30)).toString(),
                "endsAt", Instant.now().plus(Duration.ofDays(31)).toString())));
        String done = id(event(Map.of(
                "title",
                "Done",
                "startsAt",
                past.toString(),
                "endsAt",
                past.plusSeconds(3600).toString())));
        Account member = member();
        register(later, member);
        register(soon, member);
        addPeople(done, member.id());

        assertThat(titles("/api/v1/me/registrations", member)).containsExactly("Soon", "Later");
        assertThat(titles("/api/v1/me/registrations?when=PAST", member)).containsExactly("Done");
    }

    @Test
    void aNameChangeReachesTheAttendeeList() {
        String id = id(event());
        Account member = member("Old Name");
        register(id, member);

        api.patch("/api/v1/me", Map.of("name", "New Name"), member.auth());

        assertThat(attendees(id)).singleElement().satisfies(r -> assertThat(r).containsEntry("name", "New Name"));
    }

    private ResponseEntity<Map> leave(String eventId, Account who) {
        return api.delete("/api/v1/events/" + eventId + "/registrations/me", who.auth());
    }

    private ResponseEntity<Map> addPeople(String eventId, String... userIds) {
        return api.post(
                "/api/v1/events/" + eventId + "/registrations", Map.of("userIds", List.of(userIds)), admin.auth());
    }

    private List<Map<String, Object>> attendees(String eventId) {
        return api.list("/api/v1/events/" + eventId + "/registrations", admin.auth())
                .getBody();
    }

    private List<String> titles(String path, Account who) {
        return ((List<Map<String, Object>>) api.list(path, who.auth()).getBody())
                .stream().map(r -> (String) r.get("title")).toList();
    }
}
