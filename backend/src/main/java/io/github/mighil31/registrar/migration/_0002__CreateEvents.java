package io.github.mighil31.registrar.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;

/**
 * {@code events}. {@code registeredCount} is kept in step with {@code registrations} in the same
 * transaction, so the capacity check is one atomic update. {@code capacity} null means unlimited.
 */
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Change(id = "create-events", author = "registrar", transactional = false)
public class _0002__CreateEvents {

    static final String COLLECTION = "events";

    static final String SCHEMA = """
            {
              "bsonType": "object",
              "required": ["title", "format", "startsAt", "endsAt", "mode", "registeredCount", "status", "createdAt"],
              "properties": {
                "_id": { "bsonType": "objectId" },
                "title": { "bsonType": "string", "minLength": 1 },
                "description": { "bsonType": ["string", "null"] },
                "format": { "enum": ["IN_PERSON", "ONLINE"] },
                "location": { "bsonType": ["string", "null"] },
                "meetingLink": { "bsonType": ["string", "null"] },
                "startsAt": { "bsonType": "date" },
                "endsAt": { "bsonType": "date" },
                "mode": { "enum": ["OPEN", "PRIVATE"] },
                "capacity": { "bsonType": ["int", "long", "null"], "minimum": 1 },
                "registeredCount": { "bsonType": ["int", "long"], "minimum": 0 },
                "tags": { "bsonType": "array", "maxItems": 5, "items": { "bsonType": "string" } },
                "status": { "enum": ["SCHEDULED", "CANCELLED"] },
                "createdBy": { "bsonType": "objectId" },
                "createdAt": { "bsonType": "date" },
                "updatedAt": { "bsonType": "date" },
                "version": { "bsonType": ["int", "long"] }
              }
            }
            """;

    @Apply
    public void apply(MongoDatabase database) {
        Schemas.createWithValidator(database, COLLECTION, SCHEMA);
        var events = database.getCollection(COLLECTION);
        // Browse: open, scheduled, upcoming, by date.
        events.createIndex(
                Indexes.ascending("mode", "status", "startsAt"), new IndexOptions().name("mode_status_startsAt"));
        // Admin list, newest first.
        events.createIndex(Indexes.descending("startsAt"), new IndexOptions().name("startsAt_desc"));
        events.createIndex(Indexes.ascending("tags"), new IndexOptions().name("tags"));
    }

    @Rollback
    public void rollback(MongoDatabase database) {
        database.getCollection(COLLECTION).drop();
    }
}
