package io.github.mighil31.registrar;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A throwaway MongoDB in Docker, started as a single-node replica set (so transactions work);
 * {@code @ServiceConnection} points Spring at it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Keep in step with compose.yaml. */
    public static final String MONGO_IMAGE = "mongo:8.0";

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        // Testcontainers 2 no longer starts a replica set by default.
        return new MongoDBContainer(DockerImageName.parse(MONGO_IMAGE)).withReplicaSet();
    }
}
