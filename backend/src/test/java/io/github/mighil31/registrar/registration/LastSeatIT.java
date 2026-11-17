package io.github.mighil31.registrar.registration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.ApiIT;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Many members press Register at the same moment. Exactly {@code capacity} of them get a seat, the
 * rest get "full", and the stored count matches the registrations.
 */
class LastSeatIT extends ApiIT {

    static final int RACERS = 20;

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void exactlyCapacityMembersGetASeat(int capacity) throws Exception {
        String id = id(event(Map.of("capacity", capacity)));
        List<Account> racers =
                IntStream.range(0, RACERS).mapToObj(i -> member()).toList();
        CountDownLatch start = new CountDownLatch(1);

        List<HttpStatusCode> results;
        try (ExecutorService pool = Executors.newFixedThreadPool(RACERS)) {
            List<Future<HttpStatusCode>> futures = racers.stream()
                    .map(racer -> pool.submit(() -> {
                        start.await();
                        return register(id, racer).getStatusCode();
                    }))
                    .toList();
            start.countDown();
            results = futures.stream().map(LastSeatIT::get).toList();
        }

        assertThat(results).filteredOn(s -> s.equals(HttpStatus.CREATED)).hasSize(capacity);
        assertThat(results).filteredOn(s -> s.equals(HttpStatus.CONFLICT)).hasSize(RACERS - capacity);
        assertThat(getEvent(id, admin)).containsEntry("registeredCount", capacity);
        assertThat(api.list("/api/v1/events/" + id + "/registrations", admin.auth())
                        .getBody())
                .hasSize(capacity);
    }

    private static HttpStatusCode get(Future<HttpStatusCode> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
