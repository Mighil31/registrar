package io.github.mighil31.registrar;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

/** The whole app on a real server: MongoDB as a replica set, and the probes. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrarApplicationIT {

    @Autowired
    MongoTemplate mongoTemplate;

    @Value("${local.server.port}")
    int port;

    @Test
    void mongoIsAReplicaSet() {
        Document hello = mongoTemplate.executeCommand(new Document("hello", 1));

        assertThat(hello.getString("setName"))
                .as("transactions need a replica set")
                .isNotBlank();
        assertThat(hello.getBoolean("isWritablePrimary")).isTrue();
    }

    @Test
    void probesAreOnThePublicPort() throws Exception {
        for (String probe : List.of("/livez", "/readyz")) {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder(URI.create("http://localhost:" + port + probe))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).as(probe).isEqualTo(200);
            assertThat(response.body()).as(probe).contains("\"status\":\"UP\"");
        }
    }
}
