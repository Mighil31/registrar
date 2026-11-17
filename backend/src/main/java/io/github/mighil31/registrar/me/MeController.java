package io.github.mighil31.registrar.me;

import io.github.mighil31.registrar.auth.AuthService;
import io.github.mighil31.registrar.auth.InvalidRefreshTokenException;
import io.github.mighil31.registrar.auth.RefreshCookie;
import io.github.mighil31.registrar.db.MongoTransaction;
import io.github.mighil31.registrar.registration.Registrations;
import io.github.mighil31.registrar.user.User;
import io.github.mighil31.registrar.user.UserRepository;
import io.github.mighil31.registrar.user.UserResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import org.bson.types.ObjectId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own profile. */
@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasAuthority('profile:self')")
class MeController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService auth;
    private final RefreshCookie cookie;
    private final Registrations registrations;
    private final Clock clock;

    MeController(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AuthService auth,
            RefreshCookie cookie,
            Registrations registrations,
            Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auth = auth;
        this.cookie = cookie;
        this.registrations = registrations;
        this.clock = clock;
    }

    @GetMapping
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.of(current(jwt));
    }

    /** A new name also goes into the copies in this user's registrations, in the same transaction. */
    @PatchMapping
    @MongoTransaction
    UserResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        User user = current(jwt);
        User updated = user.withProfile(
                request.name() != null ? request.name() : user.name(),
                request.phoneNumber() != null ? request.phoneNumber() : user.phoneNumber(),
                clock.instant());
        User saved = users.save(updated);
        if (!saved.name().equals(user.name())) {
            registrations.copyUserName(saved.id(), saved.name());
        }
        return UserResponse.of(saved);
    }

    /** Ends every other session; this browser gets a new refresh cookie and stays signed in. */
    @PutMapping("/password")
    ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        User user = current(jwt);
        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new WrongPasswordException();
        }
        users.save(user.withPasswordHash(passwordEncoder.encode(request.newPassword()), clock.instant()));
        String refreshToken = auth.restartSessions(user.id());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.set(refreshToken))
                .build();
    }

    /** A valid token for a user who no longer exists (deleted since it was issued) is a 401. */
    private User current(Jwt jwt) {
        return users.findById(new ObjectId(jwt.getSubject())).orElseThrow(InvalidRefreshTokenException::new);
    }
}
