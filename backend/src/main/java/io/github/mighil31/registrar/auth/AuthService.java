package io.github.mighil31.registrar.auth;

import io.github.mighil31.registrar.user.User;
import io.github.mighil31.registrar.user.UserRepository;
import io.github.mighil31.registrar.user.UserResponse;
import java.time.Clock;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokens accessTokens;
    private final RefreshTokens refreshTokens;
    private final Clock clock;

    /** Compared against when the email is unknown, so both failures take as long as a real check. */
    private final String dummyHash;

    AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AccessTokens accessTokens,
            RefreshTokens refreshTokens,
            Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** A session: the response body and the refresh token for the cookie. */
    public record Session(AuthResponse response, String refreshToken) {}

    Session signUp(SignUpRequest request) {
        User user = User.newMember(
                request.name(),
                request.email(),
                request.phoneNumber(),
                passwordEncoder.encode(request.password()),
                clock.instant());
        try {
            user = users.insert(user);
        } catch (DuplicateKeyException e) {
            throw new EmailTakenException();
        }
        return newSession(user);
    }

    Session signIn(SignInRequest request) {
        User user = users.findByEmail(User.normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return newSession(user);
    }

    Session refresh(String refreshToken) {
        RefreshTokens.Rotation rotation = refreshTokens.rotate(refreshToken);
        User user = users.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);
        return new Session(response(user), rotation.token());
    }

    void signOut(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    /** After a password change: every other session ends; this one continues on a new token. */
    public String restartSessions(ObjectId userId) {
        refreshTokens.revokeAll(userId);
        return refreshTokens.issue(userId);
    }

    private Session newSession(User user) {
        return new Session(response(user), refreshTokens.issue(user.id()));
    }

    private AuthResponse response(User user) {
        return new AuthResponse(accessTokens.issue(user), accessTokens.ttlSeconds(), UserResponse.of(user));
    }
}
