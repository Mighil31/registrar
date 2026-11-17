package io.github.mighil31.registrar.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mighil31.registrar.Api;
import io.github.mighil31.registrar.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** The configured first admin exists after startup, can sign in, and gets admin permissions. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "registrar.admin.email=Priya.Anand@example.com",
            "registrar.admin.password=first-admin-password",
            "registrar.admin.name=Priya Anand"
        })
class AdminBootstrapIT {

    @Value("${local.server.port}")
    int port;

    @Autowired
    UserRepository users;

    @Autowired
    AdminBootstrap bootstrap;

    @Test
    @SuppressWarnings("unchecked")
    void firstAdminCanSignInWithAdminPermissions() {
        Map<String, Object> body = new Api(port)
                .post(
                        "/api/v1/auth/signin",
                        Map.of("email", "priya.anand@example.com", "password", "first-admin-password"))
                .getBody();

        assertThat((List<String>) Api.claims((String) body.get("accessToken")).get("perms"))
                .contains("events:manage", "roles:grant", "profile:self");
    }

    @Test
    void runningAgainChangesNothing() {
        long before = users.count();

        bootstrap.run(new DefaultApplicationArguments());

        assertThat(users.count()).isEqualTo(before);
        assertThat(users.findByEmail("priya.anand@example.com").orElseThrow().isAdmin())
                .isTrue();
    }
}
