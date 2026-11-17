package io.github.mighil31.registrar.user;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * One row of the People screen, for admins.
 *
 * @param roles in a fixed order (MEMBER, ADMIN)
 * @param eventCount how many events they're registered for, past and upcoming
 */
public record PersonResponse(
        String id, String name, String email, String phoneNumber, Set<Role> roles, int eventCount, Instant createdAt) {

    static PersonResponse of(User user, int eventCount) {
        return new PersonResponse(
                user.id().toHexString(),
                user.name(),
                user.email(),
                user.phoneNumber(),
                EnumSet.copyOf(user.roles()),
                eventCount,
                user.createdAt());
    }
}
