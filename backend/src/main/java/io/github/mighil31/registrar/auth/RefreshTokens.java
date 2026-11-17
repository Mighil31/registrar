package io.github.mighil31.registrar.auth;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Opaque refresh tokens with rotation: each use retires the token and issues the next one in the
 * same family. A retired token presented again (after the grace window) means it was copied, so the
 * whole family is revoked and both holders must sign in again.
 */
@Component
class RefreshTokens {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokens.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MongoTemplate mongo;
    private final JwtProperties properties;
    private final Clock clock;

    RefreshTokens(MongoTemplate mongo, JwtProperties properties, Clock clock) {
        this.mongo = mongo;
        this.properties = properties;
        this.clock = clock;
    }

    /** A rotated token and whose it is. */
    record Rotation(ObjectId userId, String token) {}

    /** A new family: a fresh sign-in. */
    String issue(ObjectId userId) {
        return issue(userId, UUID.randomUUID().toString());
    }

    /** Retires {@code token} and returns its successor; atomic, so a token works once. */
    Rotation rotate(String token) {
        Instant now = clock.instant();
        String hash = hash(token);
        RefreshToken current = mongo.findAndModify(
                Query.query(where("tokenHash")
                        .is(hash)
                        .and("replacedAt")
                        .isNull()
                        .and("revokedAt")
                        .isNull()
                        .and("expiresAt")
                        .gt(now)),
                Update.update("replacedAt", now),
                FindAndModifyOptions.options().returnNew(false),
                RefreshToken.class);
        if (current != null) {
            return new Rotation(current.userId(), issue(current.userId(), current.familyId()));
        }
        RefreshToken known = mongo.findOne(Query.query(where("tokenHash").is(hash)), RefreshToken.class);
        if (known != null
                && known.replacedAt() != null
                && known.replacedAt().plus(properties.reuseGrace()).isBefore(now)) {
            log.warn("Refresh token reused; revoking its family for user {}", known.userId());
            revoke(Query.query(where("familyId").is(known.familyId())), now);
        }
        throw new InvalidRefreshTokenException();
    }

    /** Sign out: this token stops working. Unknown tokens are ignored. */
    void revoke(String token) {
        revoke(Query.query(where("tokenHash").is(hash(token))), clock.instant());
    }

    /** Every session of a user: password change, role change, deletion. */
    void revokeAll(ObjectId userId) {
        revoke(Query.query(where("userId").is(userId)), clock.instant());
    }

    long ttlSeconds() {
        return properties.refreshTokenTtl().toSeconds();
    }

    private String issue(ObjectId userId, String familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        mongo.insert(new RefreshToken(
                null, hash(token), userId, familyId, now, now.plus(properties.refreshTokenTtl()), null, null));
        return token;
    }

    private void revoke(Query query, Instant now) {
        mongo.updateMulti(
                query.addCriteria(where("revokedAt").isNull()), Update.update("revokedAt", now), RefreshToken.class);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
