package io.github.mighil31.registrar.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.github.mighil31.registrar.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Each rollback undoes its apply, on a scratch database so the app's own schema is untouched. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigrationRollbackIT {

    @Autowired
    MongoClient mongoClient;

    MongoDatabase scratch;

    @AfterEach
    void dropScratch() {
        scratch.drop();
    }

    @Test
    void rollbacksInReverseOrderRemoveEveryCollection() {
        scratch = mongoClient.getDatabase("rollback_" + System.nanoTime());
        new _0001__CreateUsers().apply(scratch);
        new _0002__CreateEvents().apply(scratch);
        new _0003__CreateRegistrations().apply(scratch);
        new _0004__CreateRefreshTokens().apply(scratch);
        assertThat(collections()).containsExactlyInAnyOrder("users", "events", "registrations", "refreshTokens");

        new _0004__CreateRefreshTokens().rollback(scratch);
        new _0003__CreateRegistrations().rollback(scratch);
        new _0002__CreateEvents().rollback(scratch);
        new _0001__CreateUsers().rollback(scratch);

        assertThat(collections()).isEmpty();
    }

    private List<String> collections() {
        return scratch.listCollectionNames().into(new ArrayList<>());
    }
}
