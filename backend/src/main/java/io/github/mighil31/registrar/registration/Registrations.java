package io.github.mighil31.registrar.registration;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.github.mighil31.registrar.event.Event;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * What other parts of the app need from registrations: lookups, and keeping the copied fields in
 * step. Callers run the copy updates in the same transaction as the change they copy.
 */
@Component
public class Registrations {

    private final MongoTemplate mongo;

    Registrations(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Of {@code eventIds}, the ones {@code userId} is registered for. One indexed query. */
    public Set<ObjectId> eventsAttendedBy(ObjectId userId, Collection<ObjectId> eventIds) {
        if (eventIds.isEmpty()) {
            return Set.of();
        }
        Query query = Query.query(where("userId").is(userId).and("eventId").in(eventIds));
        query.fields().include("eventId");
        return mongo.find(query, Registration.class).stream()
                .map(Registration::eventId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isRegistered(ObjectId eventId, ObjectId userId) {
        return mongo.exists(byEventAndUser(eventId, userId), Registration.class);
    }

    /** After an event's title, start or status changed. */
    public void copyEvent(Event event) {
        mongo.updateMulti(
                Query.query(where("eventId").is(event.id())),
                new Update()
                        .set("event.title", event.title())
                        .set("event.startsAt", event.startsAt())
                        .set("event.status", event.status()),
                Registration.class);
    }

    /** After a user changed their name. */
    public void copyUserName(ObjectId userId, String name) {
        mongo.updateMulti(
                Query.query(where("userId").is(userId)), Update.update("user.name", name), Registration.class);
    }

    public void deleteForEvent(ObjectId eventId) {
        mongo.remove(Query.query(where("eventId").is(eventId)), Registration.class);
    }

    /**
     * Before a user is deleted: their registrations go and their seats are freed. One registration
     * per user per event (unique index), so each of those events loses exactly one.
     */
    public void deleteForUser(ObjectId userId) {
        Query mine = Query.query(where("userId").is(userId));
        List<ObjectId> eventIds = mongo.findDistinct(mine, "eventId", Registration.class, ObjectId.class);
        if (eventIds.isEmpty()) {
            return;
        }
        mongo.updateMulti(Query.query(where("_id").in(eventIds)), new Update().inc("registeredCount", -1), Event.class);
        mongo.remove(Query.query(where("userId").is(userId)), Registration.class);
    }

    /** How many events each of {@code userIds} is registered for; users with none are missing. */
    public Map<ObjectId, Integer> countByUser(Collection<ObjectId> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Aggregation count = Aggregation.newAggregation(
                Aggregation.match(where("userId").in(userIds)),
                Aggregation.group("userId").count().as("count"));
        return mongo.aggregate(count, Registration.class, Document.class).getMappedResults().stream()
                .collect(Collectors.toUnmodifiableMap(d -> d.getObjectId("_id"), d -> d.getInteger("count")));
    }

    static Query byEventAndUser(ObjectId eventId, ObjectId userId) {
        return Query.query(where("eventId").is(eventId).and("userId").is(userId));
    }
}
