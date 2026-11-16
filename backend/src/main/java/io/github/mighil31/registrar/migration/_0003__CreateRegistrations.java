package io.github.mighil31.registrar.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;

/**
 * {@code registrations}: one document per (event, user), a separate collection rather than arrays
 * embedded in either side (unbounded, queried from both sides). {@code user} and {@code event} carry
 * copies of the fields the two main lists show, so neither needs a {@code $lookup}.
 */
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Change(id = "create-registrations", author = "registrar", transactional = false)
public class _0003__CreateRegistrations {

    static final String COLLECTION = "registrations";

    static final String SCHEMA = """
            {
              "bsonType": "object",
              "required": ["eventId", "userId", "registeredAt", "addedBy", "overCapacity", "user", "event"],
              "properties": {
                "_id": { "bsonType": "objectId" },
                "eventId": { "bsonType": "objectId" },
                "userId": { "bsonType": "objectId" },
                "registeredAt": { "bsonType": "date" },
                "addedBy": { "bsonType": "objectId" },
                "overCapacity": { "bsonType": "bool" },
                "user": {
                  "bsonType": "object",
                  "required": ["name", "email"],
                  "properties": { "name": { "bsonType": "string" }, "email": { "bsonType": "string" } }
                },
                "event": {
                  "bsonType": "object",
                  "required": ["title", "startsAt", "status"],
                  "properties": {
                    "title": { "bsonType": "string" },
                    "startsAt": { "bsonType": "date" },
                    "status": { "enum": ["SCHEDULED", "CANCELLED"] }
                  }
                }
              }
            }
            """;

    @Apply
    public void apply(MongoDatabase database) {
        Schemas.createWithValidator(database, COLLECTION, SCHEMA);
        var registrations = database.getCollection(COLLECTION);
        // No double registration.
        registrations.createIndex(
                Indexes.ascending("eventId", "userId"),
                new IndexOptions().name("eventId_userId_unique").unique(true));
        // My events, by date, without a lookup.
        registrations.createIndex(
                Indexes.ascending("userId", "event.startsAt"), new IndexOptions().name("userId_eventStartsAt"));
        // An event's attendee list, in registration order.
        registrations.createIndex(
                Indexes.ascending("eventId", "registeredAt"), new IndexOptions().name("eventId_registeredAt"));
        // Dashboard: most recent registrations.
        registrations.createIndex(Indexes.descending("registeredAt"), new IndexOptions().name("registeredAt_desc"));
    }

    @Rollback
    public void rollback(MongoDatabase database) {
        database.getCollection(COLLECTION).drop();
    }
}
