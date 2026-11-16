package io.github.mighil31.registrar.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.flamingock.api.annotations.Apply;
import io.flamingock.api.annotations.Change;
import io.flamingock.api.annotations.Rollback;
import io.flamingock.api.annotations.TargetSystem;
import java.util.concurrent.TimeUnit;

/**
 * {@code refreshTokens}: only a SHA-256 hash of each token is stored. A TTL index on {@code expiresAt}
 * lets MongoDB delete expired tokens itself (expireAfterSeconds 0 = at the stored time).
 */
@TargetSystem(id = FlamingockConfig.TARGET_SYSTEM)
@Change(id = "create-refresh-tokens", author = "registrar", transactional = false)
public class _0004__CreateRefreshTokens {

    static final String COLLECTION = "refreshTokens";

    static final String SCHEMA = """
            {
              "bsonType": "object",
              "required": ["tokenHash", "userId", "familyId", "createdAt", "expiresAt"],
              "properties": {
                "_id": { "bsonType": "objectId" },
                "tokenHash": { "bsonType": "string", "minLength": 1 },
                "userId": { "bsonType": "objectId" },
                "familyId": { "bsonType": "string" },
                "createdAt": { "bsonType": "date" },
                "expiresAt": { "bsonType": "date" },
                "replacedAt": { "bsonType": ["date", "null"] },
                "revokedAt": { "bsonType": ["date", "null"] }
              }
            }
            """;

    @Apply
    public void apply(MongoDatabase database) {
        Schemas.createWithValidator(database, COLLECTION, SCHEMA);
        var tokens = database.getCollection(COLLECTION);
        tokens.createIndex(
                Indexes.ascending("tokenHash"),
                new IndexOptions().name("tokenHash_unique").unique(true));
        tokens.createIndex(
                Indexes.ascending("expiresAt"),
                new IndexOptions().name("expiresAt_ttl").expireAfter(0L, TimeUnit.SECONDS));
        // Revoke a whole family (reuse detected), or all of a user's tokens (role change, delete).
        tokens.createIndex(Indexes.ascending("familyId"), new IndexOptions().name("familyId"));
        tokens.createIndex(Indexes.ascending("userId"), new IndexOptions().name("userId"));
    }

    @Rollback
    public void rollback(MongoDatabase database) {
        database.getCollection(COLLECTION).drop();
    }
}
