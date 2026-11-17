package io.github.mighil31.registrar.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.ApiIT;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Other tests share this database, so the numbers are checked as changes: what one new member,
 * one upcoming event and one registration add.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class DashboardIT extends ApiIT {

    @Test
    void countsAndListsWhatJustHappened() {
        Map before = dashboard();
        Instant soon = Instant.now().plus(Duration.ofMinutes(5));
        Map<String, Object> event = event(Map.of(
                "title",
                "Dashboard check",
                "capacity",
                4,
                "startsAt",
                soon.toString(),
                "endsAt",
                soon.plusSeconds(3600).toString()));
        Account member = member("Rhea Dashwood");
        register(id(event), member);
        Instant past = Instant.now().minus(Duration.ofDays(1));
        event(Map.of(
                "startsAt", past.toString(), "endsAt", past.plusSeconds(3600).toString()));
        String cancelled = id(event());
        api.post("/api/v1/events/" + cancelled + "/cancel", null, admin.auth());
        newAdmin("Ad Ministrator");

        Map after = dashboard();

        assertThat(number(after, "members")).isEqualTo(number(before, "members") + 1);
        assertThat(number(after, "upcomingEvents")).isEqualTo(number(before, "upcomingEvents") + 1);
        assertThat(number(after, "registrationsThisWeek")).isEqualTo(number(before, "registrationsThisWeek") + 1);
        assertThat((Map) ((List) after.get("recentRegistrations")).getFirst())
                .containsEntry("eventId", id(event))
                .containsEntry("eventTitle", "Dashboard check")
                .containsEntry("userId", member.id())
                .containsEntry("userName", "Rhea Dashwood");
        assertThat((List<Map>) after.get("events"))
                .filteredOn(row -> row.get("id").equals(id(event)))
                .singleElement()
                .satisfies(row -> assertThat(row)
                        .containsEntry("registeredCount", 1)
                        .containsEntry("capacity", 4)
                        .containsEntry("percentFull", 25)
                        .containsEntry("mode", "OPEN"));
        assertThat((List) after.get("events")).hasSizeLessThanOrEqualTo(DashboardService.EVENT_ROWS);
        assertThat((List) after.get("recentRegistrations"))
                .hasSizeLessThanOrEqualTo(DashboardService.RECENT_REGISTRATIONS);
    }

    @Test
    void adminsOnly() {
        assertThat(api.get("/api/v1/dashboard", member().auth()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.get("/api/v1/dashboard").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Map dashboard() {
        return api.get("/api/v1/dashboard", admin.auth()).getBody();
    }

    private static long number(Map dashboard, String name) {
        return ((Number) dashboard.get(name)).longValue();
    }
}
