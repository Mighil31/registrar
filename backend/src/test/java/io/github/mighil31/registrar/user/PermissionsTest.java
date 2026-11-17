package io.github.mighil31.registrar.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PermissionsTest {

    @Test
    void memberCanOnlyActForThemselves() {
        assertThat(Permissions.of(List.of(Role.MEMBER)))
                .containsExactly("events:browse", "profile:self", "registrations:self");
    }

    @Test
    void adminAlsoManagesEverything() {
        assertThat(Permissions.of(List.of(Role.MEMBER, Role.ADMIN)))
                .contains("profile:self", "events:manage", "registrations:manage", "users:manage", "roles:grant")
                .hasSize(9);
    }
}
