package io.github.mighil31.registrar.auth;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A refresh token as stored: only its SHA-256 hash, never the token itself. All tokens descended
 * from one sign-in share a {@code familyId}. The TTL index on {@code expiresAt} deletes it later.
 */
@Document("refreshTokens")
record RefreshToken(
        @Id ObjectId id,
        String tokenHash,
        ObjectId userId,
        String familyId,
        Instant createdAt,
        Instant expiresAt,
        Instant replacedAt,
        Instant revokedAt) {}
