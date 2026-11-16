package io.github.mighil31.registrar.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import io.github.mighil31.registrar.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

/** The migrations ran at startup and the schema enforces what the app relies on. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SchemaMigrationsIT {

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void emptyCollections() {
        // deleteMany, not drop: dropping would also drop the indexes and validators under test.
        for (String name : List.of("users", "events", "registrations", "refreshTokens")) {
            mongoTemplate.getCollection(name).deleteMany(new Document());
        }
    }

    @Test
    void auditRecordsEverySchemaChange() {
        List<String> applied = mongoTemplate
                .getCollection("flamingockAuditLog")
                .find(new Document("state", "APPLIED"))
                .map(entry -> entry.getString("changeId"))
                .into(new ArrayList<>());

        assertThat(applied)
                .containsExactlyInAnyOrder(
                        "create-users", "create-events", "create-registrations", "create-refresh-tokens");
    }

    @Test
    void emailIsUnique() {
        users().insertOne(user("jane@example.com"));

        assertThatThrownBy(() -> users().insertOne(user("jane@example.com")))
                .isInstanceOf(MongoWriteException.class)
                .hasMessageContaining("E11000");
    }

    @Test
    void userValidatorRejectsUnknownRole() {
        Document user = user("bob@example.com").append("roles", List.of("SUPERUSER"));

        assertThatThrownBy(() -> users().insertOne(user)).hasMessageContaining("Document failed validation");
    }

    @Test
    void registrationIsUniquePerEventAndUser() {
        ObjectId eventId = new ObjectId();
        ObjectId userId = new ObjectId();
        registrations().insertOne(registration(eventId, userId));

        assertThatThrownBy(() -> registrations().insertOne(registration(eventId, userId)))
                .isInstanceOf(MongoWriteException.class)
                .hasMessageContaining("E11000");
        registrations().insertOne(registration(new ObjectId(), userId));
        assertThat(registrations().countDocuments()).isEqualTo(2);
    }

    @Test
    void eventValidatorRejectsNegativeCountAndUnknownMode() {
        assertThatThrownBy(() -> events().insertOne(event().append("registeredCount", -1)))
                .hasMessageContaining("Document failed validation");
        assertThatThrownBy(() -> events().insertOne(event().append("mode", "SECRET")))
                .hasMessageContaining("Document failed validation");
        events().insertOne(event());
    }

    @Test
    void refreshTokensExpireByTtlIndex() {
        Document ttl = index("refreshTokens", "expiresAt_ttl");

        assertThat(ttl.get("key", Document.class)).isEqualTo(new Document("expiresAt", 1));
        assertThat(((Number) ttl.get("expireAfterSeconds")).longValue()).isZero();
        assertThat(index("refreshTokens", "tokenHash_unique").getBoolean("unique"))
                .isTrue();
    }

    @Test
    void indexesForTheMainQueriesExist() {
        assertThat(indexNames("users")).contains("email_unique", "roles_name", "name");
        assertThat(indexNames("events")).contains("mode_status_startsAt", "startsAt_desc", "tags");
        assertThat(indexNames("registrations"))
                .contains("eventId_userId_unique", "userId_eventStartsAt", "eventId_registeredAt", "registeredAt_desc");
        assertThat(indexNames("refreshTokens")).contains("familyId", "userId");
    }

    private MongoCollection<Document> users() {
        return mongoTemplate.getCollection("users");
    }

    private MongoCollection<Document> events() {
        return mongoTemplate.getCollection("events");
    }

    private MongoCollection<Document> registrations() {
        return mongoTemplate.getCollection("registrations");
    }

    private Document index(String collection, String name) {
        return mongoTemplate.getCollection(collection).listIndexes().into(new ArrayList<>()).stream()
                .filter(index -> name.equals(index.getString("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no index " + name + " on " + collection));
    }

    private List<String> indexNames(String collection) {
        return mongoTemplate
                .getCollection(collection)
                .listIndexes()
                .map(index -> index.getString("name"))
                .into(new ArrayList<>());
    }

    private static Document user(String email) {
        return new Document("name", "Jane Doe")
                .append("email", email)
                .append("phoneNumber", "2125551234")
                .append("passwordHash", "{bcrypt}x")
                .append("roles", List.of("MEMBER"))
                .append("createdAt", new Date());
    }

    private static Document event() {
        Date now = new Date();
        return new Document("title", "Board Game Night")
                .append("format", "IN_PERSON")
                .append("startsAt", now)
                .append("endsAt", now)
                .append("mode", "OPEN")
                .append("registeredCount", 0)
                .append("status", "SCHEDULED")
                .append("createdAt", now);
    }

    private static Document registration(ObjectId eventId, ObjectId userId) {
        return new Document("eventId", eventId)
                .append("userId", userId)
                .append("registeredAt", new Date())
                .append("addedBy", userId)
                .append("overCapacity", false)
                .append("user", new Document("name", "Jane Doe").append("email", "jane@example.com"))
                .append(
                        "event",
                        new Document("title", "Board Game Night")
                                .append("startsAt", new Date())
                                .append("status", "SCHEDULED"));
    }
}
