package io.github.mighil31.registrar.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtConfigTest {

    @Test
    void readsAConfiguredPemKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();

        KeyPair read = JwtConfig.keyPair(properties(
                pem("PRIVATE KEY", keys.getPrivate().getEncoded()),
                pem("PUBLIC KEY", keys.getPublic().getEncoded())));

        assertThat(read.getPublic().getEncoded()).isEqualTo(keys.getPublic().getEncoded());
        assertThat(read.getPrivate().getEncoded()).isEqualTo(keys.getPrivate().getEncoded());
    }

    @Test
    void generatesAKeyPairWhenNoneIsConfigured() {
        assertThat(JwtConfig.keyPair(properties(null, null)).getPublic().getAlgorithm())
                .isEqualTo("RSA");
    }

    @Test
    void refusesHalfAKeyPair() {
        assertThatThrownBy(() -> JwtConfig.keyPair(properties("x", null))).hasMessageContaining("both");
    }

    private static JwtProperties properties(String privateKey, String publicKey) {
        return new JwtProperties(
                "registrar",
                Duration.ofMinutes(10),
                Duration.ofDays(14),
                Duration.ofSeconds(10),
                privateKey,
                publicKey);
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder().encodeToString(der) + "\n-----END " + type
                + "-----\n";
    }
}
