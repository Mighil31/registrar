package io.github.mighil31.registrar.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.ApiIT;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

/** The OpenAPI document is public, covers the API, and says which calls need a token. */
@SuppressWarnings({"rawtypes", "unchecked"})
class OpenApiIT extends ApiIT {

    @Test
    void describesTheApiAndItsBearerTokens() throws Exception {
        ResponseEntity<Map> response = api.get("/v3/api-docs");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // The web app's TypeScript types are generated from this file (npm run api in web/).
        Files.writeString(Path.of("target", "openapi.json"), new JsonMapper().writeValueAsString(response.getBody()));
        Map spec = response.getBody();
        Map<String, Map> paths = (Map<String, Map>) spec.get("paths");
        assertThat(paths)
                .containsKeys(
                        "/api/v1/auth/signin",
                        "/api/v1/events/{id}",
                        "/api/v1/users",
                        "/api/v1/users/{id}/roles",
                        "/api/v1/dashboard")
                .allSatisfy((path, item) -> assertThat(path).startsWith("/api/v1/"));
        assertThat((Map) ((Map) spec.get("components")).get("securitySchemes"))
                .containsEntry("bearer", Map.of("type", "http", "scheme", "bearer", "bearerFormat", "JWT"));
        assertThat(spec.get("security")).isEqualTo(List.of(Map.of("bearer", List.of())));
        Map signIn = (Map) paths.get("/api/v1/auth/signin").get("post");
        assertThat(signIn.get("security")).isEqualTo(List.of());
        Map revoke = (Map) paths.get("/api/v1/users/{id}/roles").get("put");
        assertThat((Map) ((Map) revoke.get("responses")).get("default"))
                .extractingByKey("content")
                .isEqualTo(Map.of(
                        "application/problem+json", Map.of("schema", Map.of("$ref", "#/components/schemas/Problem"))));
    }
}
