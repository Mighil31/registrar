package io.github.mighil31.registrar.migration;

import com.mongodb.client.MongoClient;
import io.flamingock.api.annotations.EnableFlamingock;
import io.flamingock.api.annotations.Stage;
import io.flamingock.store.mongodb.sync.MongoDBSyncAuditStore;
import io.flamingock.targetsystem.mongodb.sync.MongoDBSyncTargetSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;

/**
 * Flamingock on the plain MongoDB driver. Changes in this package run in name order ({@code _0001__…})
 * before the web server starts, each once (recorded in an audit collection). Never edit a change
 * that has shipped; add a new one.
 */
@Configuration(proxyBeanMethods = false)
@EnableFlamingock(stages = @Stage(name = "schema", location = "io.github.mighil31.registrar.migration"))
public class FlamingockConfig {

    public static final String TARGET_SYSTEM = "registrar-mongodb";

    @Bean
    MongoDBSyncTargetSystem mongoTargetSystem(MongoClient mongoClient, MongoDatabaseFactory databaseFactory) {
        String database = databaseFactory.getMongoDatabase().getName();
        return new MongoDBSyncTargetSystem(TARGET_SYSTEM, mongoClient, database);
    }

    @Bean
    MongoDBSyncAuditStore auditStore(MongoDBSyncTargetSystem targetSystem) {
        return MongoDBSyncAuditStore.from(targetSystem);
    }
}
