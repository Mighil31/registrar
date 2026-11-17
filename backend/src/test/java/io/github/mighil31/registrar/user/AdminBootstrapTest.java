package io.github.mighil31.registrar.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

class AdminBootstrapTest {

    final UserRepository users = mock(UserRepository.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void doesNothingOnceAnAdminExists() {
        when(users.existsByRoles(Role.ADMIN)).thenReturn(true);

        run(new AdminProperties("a@example.com", "long-enough-password", null, null));

        verify(users, never()).insert(any(User.class));
        verify(users, never()).save(any(User.class));
    }

    @Test
    void doesNothingWithoutAnEmail() {
        run(new AdminProperties(" ", null, null, null));

        verify(users, never()).insert(any(User.class));
    }

    @Test
    void promotesAnExistingUser() {
        User member = new User(
                new ObjectId(), "Jane", "jane@example.com", "2125551234", "x", Set.of(Role.MEMBER), null, null, 1L);
        when(users.findByEmail("jane@example.com")).thenReturn(Optional.of(member));

        run(new AdminProperties("Jane@Example.com", null, null, null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().roles()).containsExactlyInAnyOrder(Role.MEMBER, Role.ADMIN);
    }

    @Test
    void createsTheAdminWithDefaults() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        run(new AdminProperties("admin@example.com", "long-enough-password", null, null));

        ArgumentCaptor<User> inserted = ArgumentCaptor.forClass(User.class);
        verify(users).insert(inserted.capture());
        assertThat(inserted.getValue().name()).isEqualTo("Admin");
        assertThat(inserted.getValue().isAdmin()).isTrue();
        assertThat(inserted.getValue().passwordHash()).startsWith("{bcrypt}");
    }

    @Test
    void refusesAShortPassword() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> run(new AdminProperties("admin@example.com", "short", null, null)))
                .hasMessageContaining("at least 12");
    }

    private void run(AdminProperties properties) {
        new AdminBootstrap(properties, users, PasswordEncoderFactories.createDelegatingPasswordEncoder(), clock)
                .run(new DefaultApplicationArguments());
    }
}
