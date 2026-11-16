package io.github.mighil31.registrar.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;

/**
 * {@code users}: everyone who signed up. Email is unique; it's stored lower-cased, so uniqueness
 * ignores case without a collation. {@code roles + name} serves "list admins, by name".
 */
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Change(id = "create-users", author = "registrar", transactional = false)
public class _0001__CreateUsers {

    static final String COLLECTION = "users";

    static final String SCHEMA = """
            {
              "bsonType": "object",
              "required": ["name", "email", "phoneNumber", "passwordHash", "roles", "createdAt"],
              "properties": {
                "_id": { "bsonType": "objectId" },
                "name": { "bsonType": "string", "minLength": 1 },
                "email": { "bsonType": "string", "minLength": 3 },
                "phoneNumber": { "bsonType": "string", "minLength": 1 },
                "passwordHash": { "bsonType": "string", "minLength": 1 },
                "roles": { "bsonType": "array", "minItems": 1, "items": { "enum": ["MEMBER", "ADMIN"] } },
                "createdAt": { "bsonType": "date" },
                "updatedAt": { "bsonType": "date" },
                "version": { "bsonType": ["int", "long"] }
              }
            }
            """;

    @Apply
    public void apply(MongoDatabase database) {
        Schemas.createWithValidator(database, COLLECTION, SCHEMA);
        var users = database.getCollection(COLLECTION);
        users.createIndex(
                Indexes.ascending("email"),
                new IndexOptions().name("email_unique").unique(true));
        users.createIndex(Indexes.ascending("roles", "name"), new IndexOptions().name("roles_name"));
        users.createIndex(Indexes.ascending("name"), new IndexOptions().name("name"));
    }

    @Rollback
    public void rollback(MongoDatabase database) {
        database.getCollection(COLLECTION).drop();
    }
}
