package io.github.mighil31.registrar.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param privateKey PKCS#8 PEM. With neither key set, a key pair is generated at startup (dev only:
 *     access tokens then die with the process, but the refresh cookie gets a new one silently)
 * @param publicKey X.509 PEM, matching {@code privateKey}
 * @param reuseGrace a refresh token presented again within this window counts as two tabs refreshing
 *     at once, not as theft: refused, but its family isn't revoked
 */
@ConfigurationProperties("registrar.jwt")
public record JwtProperties(
        @DefaultValue("registrar") String issuer,
        @DefaultValue("10m") Duration accessTokenTtl,
        @DefaultValue("14d") Duration refreshTokenTtl,
        @DefaultValue("10s") Duration reuseGrace,
        String privateKey,
        String publicKey) {}
