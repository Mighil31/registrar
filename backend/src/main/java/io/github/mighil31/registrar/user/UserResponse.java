package io.github.mighil31.registrar.user;

import java.util.Set;

/** A user as the API shows it: never the password hash. */
public record UserResponse(String id, String name, String email, String phoneNumber, Set<Role> roles) {

    public static UserResponse of(User user) {
        return new UserResponse(user.id().toHexString(), user.name(), user.email(), user.phoneNumber(), user.roles());
    }
}
