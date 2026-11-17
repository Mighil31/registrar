package io.github.mighil31.registrar.dashboard;

import io.github.mighil31.registrar.event.Event;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;

/**
 * The whole dashboard in one aggregation, so one round trip to MongoDB.
 *
 * <p>The data lives in three collections. The pipeline starts on {@code users} and pulls in the
 * other two with {@code $unionWith}, so one stream carries five kinds of document, each tagged with
 * a {@code part}. Every sub-pipeline does its own filtering, sorting and limiting first, where it can
 * use that collection's indexes. Then {@code $facet} splits the stream back into the five parts of
 * the response: three counts and two short lists.
 *
 * <p>Written as plain stages ({@code Document}s) rather than Spring's aggregation builders: it reads
 * like the mongosh version and can be pasted into it.
 */
@Service
class DashboardService {

    static final int EVENT_ROWS = 10;
    static final int RECENT_REGISTRATIONS = 10;

    private final MongoTemplate mongo;
    private final Clock clock;

    DashboardService(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    Dashboard load() {
        Instant now = clock.instant();
        Document upcoming = new Document("status", Event.Status.SCHEDULED.name())
                .append("startsAt", new Document("$gt", Date.from(now)));
        Date weekAgo = Date.from(now.minus(Duration.ofDays(7)));

        List<Document> stages = List.of(
                // users: one small document per member who isn't an admin, just to count them.
                stage("$match", new Document("roles", new Document("$ne", "ADMIN"))),
                stage("$project", part("members").append("_id", 0)),
                // events: count the upcoming ones (mode_status_startsAt / startsAt_desc)...
                unionWith(
                        "events",
                        stage("$match", upcoming),
                        stage("$project", part("upcoming").append("_id", 0))),
                // ...and the soonest few for the table.
                unionWith(
                        "events",
                        stage("$match", upcoming),
                        stage("$sort", new Document("startsAt", 1)),
                        stage("$limit", EVENT_ROWS),
                        stage(
                                "$project",
                                part("event")
                                        .append("title", 1)
                                        .append("startsAt", 1)
                                        .append("mode", 1)
                                        .append("format", 1)
                                        .append("location", 1)
                                        .append("tags", 1)
                                        .append("capacity", 1)
                                        .append("registeredCount", 1))),
                // registrations (registeredAt_desc): count this week's...
                unionWith(
                        "registrations",
                        stage("$match", new Document("registeredAt", new Document("$gte", weekAgo))),
                        stage("$project", part("week").append("_id", 0))),
                // ...and the latest few, from the copied names (no $lookup).
                unionWith(
                        "registrations",
                        stage("$sort", new Document("registeredAt", -1)),
                        stage("$limit", RECENT_REGISTRATIONS),
                        stage(
                                "$project",
                                part("recent")
                                        .append("_id", 0)
                                        .append("eventId", 1)
                                        .append("userId", 1)
                                        .append("registeredAt", 1)
                                        .append("userName", "$user.name")
                                        .append("eventTitle", "$event.title")
                                        .append("addedByAdmin", new Document("$ne", List.of("$addedBy", "$userId"))))),
                stage(
                        "$facet",
                        new Document("members", count("members"))
                                .append("upcoming", count("upcoming"))
                                .append("week", count("week"))
                                .append("events", only("event", new Document("startsAt", 1)))
                                .append("recent", only("recent", new Document("registeredAt", -1)))));

        Aggregation aggregation = Aggregation.newAggregation(
                stages.stream().map(Aggregation::stage).toList());
        Document result = mongo.aggregate(aggregation, "users", Document.class).getUniqueMappedResult();
        return toDashboard(result);
    }

    private static Dashboard toDashboard(Document facets) {
        return new Dashboard(
                total(facets, "members"),
                total(facets, "upcoming"),
                total(facets, "week"),
                facets.getList("events", Document.class).stream()
                        .map(DashboardService::eventRow)
                        .toList(),
                facets.getList("recent", Document.class).stream()
                        .map(DashboardService::recent)
                        .toList());
    }

    private static Dashboard.EventRow eventRow(Document e) {
        Integer capacity = e.getInteger("capacity");
        int registered = e.getInteger("registeredCount");
        return new Dashboard.EventRow(
                e.getObjectId("_id").toHexString(),
                e.getString("title"),
                e.getDate("startsAt").toInstant(),
                Event.Mode.valueOf(e.getString("mode")),
                Event.Format.valueOf(e.getString("format")),
                e.getString("location"),
                e.getList("tags", String.class, List.of()),
                capacity,
                registered,
                capacity == null ? null : (int) Math.round(100.0 * registered / capacity));
    }

    private static Dashboard.RecentRegistration recent(Document r) {
        return new Dashboard.RecentRegistration(
                r.getObjectId("eventId").toHexString(),
                r.getString("eventTitle"),
                r.getObjectId("userId").toHexString(),
                r.getString("userName"),
                r.getDate("registeredAt").toInstant(),
                r.getBoolean("addedByAdmin"));
    }

    /** {@code $count} on no documents outputs nothing at all, hence the empty case. */
    private static long total(Document facets, String name) {
        List<Document> counted = facets.getList(name, Document.class);
        return counted.isEmpty() ? 0 : counted.getFirst().getInteger("n");
    }

    private static Document stage(String operator, Object argument) {
        return new Document(operator, argument);
    }

    private static Document part(String name) {
        return new Document("part", new Document("$literal", name));
    }

    private static Document unionWith(String collection, Document... pipeline) {
        return stage("$unionWith", new Document("coll", collection).append("pipeline", List.of(pipeline)));
    }

    private static List<Document> count(String part) {
        return List.of(stage("$match", new Document("part", part)), stage("$count", "n"));
    }

    private static List<Document> only(String part, Document sort) {
        return List.of(
                stage("$match", new Document("part", part)),
                stage("$sort", sort),
                stage("$project", new Document("part", 0)));
    }
}
