package io.github.mighil31.registrar.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.ApiIT;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Pairs of admins revoke each other at the same moment. Each pair must keep exactly one admin: the
 * write skew the caller lock in {@code UserService} prevents would leave both demoted.
 */
@SuppressWarnings("rawtypes")
class LastAdminRaceIT extends ApiIT {

    static final int PAIRS = 10;

    @Test
    void twoAdminsRevokingEachOtherLeaveOneAdmin() throws Exception {
        List<Account[]> pairs = IntStream.range(0, PAIRS)
                .mapToObj(i -> new Account[] {newAdmin("Ada Lovelace"), newAdmin("Bea Morgan")})
                .toList();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(PAIRS * 2)) {
            List<Future<HttpStatusCode>> futures = pairs.stream()
                    .flatMap(pair -> Stream.of(
                            pool.submit(() -> revoke(start, pair[1], pair[0])),
                            pool.submit(() -> revoke(start, pair[0], pair[1]))))
                    .toList();
            start.countDown();
            for (Future<HttpStatusCode> future : futures) {
                assertThat(future.get()).isIn(HttpStatus.OK, HttpStatus.FORBIDDEN);
            }
        }

        for (Account[] pair : pairs) {
            long stillAdmin = Arrays.stream(pair)
                    .map(a -> api.get("/api/v1/users/" + a.id(), admin.auth()).getBody())
                    .filter(person -> ((List) person.get("roles")).contains("ADMIN"))
                    .count();
            assertThat(stillAdmin).as("admins left in a pair").isEqualTo(1);
        }
    }

    /** {@code who} revokes {@code target}'s admin role. */
    private HttpStatusCode revoke(CountDownLatch start, Account target, Account who) throws InterruptedException {
        start.await();
        Map<String, Object> body = Map.of("roles", List.of("MEMBER"));
        return api.put("/api/v1/users/" + target.id() + "/roles", body, who.auth())
                .getStatusCode();
    }
}
