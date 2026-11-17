package io.github.mighil31.registrar.user;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Someone who signed up, in {@code users}. Indexes and the validator are migrations
 * ({@code _0001__CreateUsers}), not annotations.
 *
 * @param version optimistic locking: a save based on a stale copy fails instead of overwriting
 */
@Document("users")
public record User(
        @Id ObjectId id,
        String name,
        String email,
        String phoneNumber,
        String passwordHash,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt,
        @Version Long version) {

    public static User newMember(String name, String email, String phoneNumber, String passwordHash, Instant now) {
        return new User(
                null, name, normalizeEmail(email), phoneNumber, passwordHash, Set.of(Role.MEMBER), now, now, null);
    }

    /**
     * Lower-cased, so the unique index treats {@code Jane@x.com} and {@code jane@x.com} as one address.
     * {@code Locale.ROOT}, so the result doesn't depend on the server's locale (Turkish dotless i).
     */
    public static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }

    public User withProfile(String name, String phoneNumber, Instant now) {
        return new User(id, name, email, phoneNumber, passwordHash, roles, createdAt, now, version);
    }

    public User withPasswordHash(String passwordHash, Instant now) {
        return new User(id, name, email, phoneNumber, passwordHash, roles, createdAt, now, version);
    }

    public User withRoles(Set<Role> roles, Instant now) {
        return new User(id, name, email, phoneNumber, passwordHash, Set.copyOf(roles), createdAt, now, version);
    }
}
