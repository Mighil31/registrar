package io.github.mighil31.registrar.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.github.mighil31.registrar.ApiIT;
import io.github.mighil31.registrar.event.Event;
import io.github.mighil31.registrar.registration.Registration;
import io.github.mighil31.registrar.user.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/** Its own database (the seed only runs on an empty one), with the seed switched on. */
@TestPropertySource(properties = "registrar.dev-seed=true")
@SuppressWarnings({"rawtypes", "unchecked"})
class DevSeedIT extends ApiIT {

    @Autowired
    MongoTemplate mongo;

    @Autowired
    DevSeed seed;

    @Test
    void seedsConsistentDataOnceBesideTheFirstAdmin() {
        List<Event> events = mongo.findAll(Event.class);

        assertThat(mongo.count(new Query(), User.class))
                .as("the first admin and 40 seeded")
                .isEqualTo(41);
        assertThat(mongo.count(Query.query(where("roles").is("ADMIN")), User.class))
                .isEqualTo(2);
        assertThat(events).hasSize(12);
        for (Event event : events) {
            long going = mongo.count(Query.query(where("eventId").is(event.id())), Registration.class);
            assertThat(event.registeredCount()).as(event.title()).isEqualTo(going);
            assertThat(event.capacity() == null || going <= event.capacity()).isTrue();
        }
        assertThat(events).filteredOn(Event::isFull).isNotEmpty();
        assertThat(api.post(
                                "/api/v1/auth/signin",
                                Map.of("email", DevSeed.email("Liam O'Connor"), "password", DevSeed.PASSWORD))
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);

        Map dashboard = api.get("/api/v1/dashboard", admin.auth()).getBody();
        assertThat(dashboard).containsEntry("members", 39).containsEntry("upcomingEvents", 8);
        assertThat(((Number) dashboard.get("registrationsThisWeek")).intValue()).isPositive();
        assertThat((List) dashboard.get("recentRegistrations")).hasSize(10);
        assertThat((List<Map>) dashboard.get("events"))
                .extracting(row -> row.get("title"))
                .startsWith("Schema design clinic")
                .doesNotContain("Accessibility review", "Kubernetes basics");

        seed.run(new DefaultApplicationArguments());
        assertThat(mongo.count(new Query(), Event.class)).isEqualTo(12);

        // Every event deleted: still seeded, so a restart skips instead of colliding on emails.
        mongo.remove(new Query(), Registration.class);
        mongo.remove(new Query(), Event.class);
        seed.run(new DefaultApplicationArguments());
        assertThat(mongo.count(new Query(), Event.class)).isZero();
        assertThat(mongo.count(new Query(), User.class)).isEqualTo(41);
    }

    @Test
    void emailsAreAsciiAndLowerCase() {
        assertThat(DevSeed.email("Chloé Martin")).isEqualTo("chloe.martin@registrar.example");
        assertThat(DevSeed.email("Liam O'Connor")).isEqualTo("liam.oconnor@registrar.example");
        assertThat(DevSeed.MARKER_EMAIL).isEqualTo("dana.whitfield@registrar.example");
    }
}
