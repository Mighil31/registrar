package io.github.mighil31.registrar;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** A thin HTTP client for the tests: never throws on 4xx/5xx, returns bodies as maps. */
public final class Api {

    public static final String CLIENT_HEADER = "X-Requested-With";

    private final RestClient http;

    public Api(int port) {
        this.http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    @SuppressWarnings("rawtypes")
    public ResponseEntity<Map> post(String path, Object body, String... headers) {
        return http.post()
                .uri(path)
                .headers(h -> add(h, headers))
                .body(body == null ? Map.of() : body)
                .retrieve()
                .toEntity(Map.class);
    }

    @SuppressWarnings("rawtypes")
    public ResponseEntity<Map> get(String path, String... headers) {
        return http.get().uri(path).headers(h -> add(h, headers)).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("rawtypes")
    public ResponseEntity<Map> patch(String path, Object body, String... headers) {
        return http.patch()
                .uri(path)
                .headers(h -> add(h, headers))
                .body(body)
                .retrieve()
                .toEntity(Map.class);
    }

    @SuppressWarnings("rawtypes")
    public ResponseEntity<Map> put(String path, Object body, String... headers) {
        return http.put()
                .uri(path)
                .headers(h -> add(h, headers))
                .body(body)
                .retrieve()
                .toEntity(Map.class);
    }

    @SuppressWarnings("rawtypes")
    public ResponseEntity<Map> delete(String path, String... headers) {
        return http.delete().uri(path).headers(h -> add(h, headers)).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("rawtypes")
    public ResponseEntity<List> list(String path, String... headers) {
        return http.get().uri(path).headers(h -> add(h, headers)).retrieve().toEntity(List.class);
    }

    public static String[] bearer(String token) {
        return new String[] {HttpHeaders.AUTHORIZATION, "Bearer " + token};
    }

    /** The refresh cookie's header line, or empty. */
    public static Optional<String> refreshCookie(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
        return cookies.stream().filter(c -> c.startsWith("registrar_refresh=")).findFirst();
    }

    /** The refresh token's value, to send back as {@code Cookie: registrar_refresh=...}. */
    public static String refreshToken(ResponseEntity<?> response) {
        String cookie = refreshCookie(response).orElseThrow();
        return cookie.substring("registrar_refresh=".length(), cookie.indexOf(';'));
    }

    public static String[] withCookie(String token) {
        return new String[] {HttpHeaders.COOKIE, "registrar_refresh=" + token, CLIENT_HEADER, "XMLHttpRequest"};
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> claims(String jwt) {
        byte[] payload = Base64.getUrlDecoder().decode(jwt.split("\\.")[1]);
        return new JsonMapper().readValue(payload, Map.class);
    }

    private static void add(HttpHeaders headers, String... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            headers.add(pairs[i], pairs[i + 1]);
        }
    }
}
