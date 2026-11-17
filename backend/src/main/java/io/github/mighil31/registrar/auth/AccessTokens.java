package io.github.mighil31.registrar.auth;

import io.github.mighil31.registrar.user.Permissions;
import io.github.mighil31.registrar.user.User;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Short-lived JWTs. {@code perms} becomes the request's authorities; {@code roles} and {@code name}
 * are for the UI. A role change reaches a user at their next refresh (at most the token's lifetime).
 */
@Component
class AccessTokens {

    static final String PERMISSIONS_CLAIM = "perms";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    AccessTokens(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    String issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.id().toHexString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("name", user.name())
                .claim("roles", user.roles().stream().map(Enum::name).sorted().toList())
                .claim(PERMISSIONS_CLAIM, Permissions.of(user.roles()))
                .build();
        JwsHeader header =
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(JwtConfig.KEY_ID).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    long ttlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }
}
