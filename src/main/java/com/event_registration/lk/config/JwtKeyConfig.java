package com.event_registration.lk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Set;

/**
 * Resolves the RSA key pair used for RS256 token signing and verification.
 *
 * <p>Production: both keys MUST be supplied as PEM via environment-backed
 * properties ({@code jwt.rsa.private-key} / {@code jwt.rsa.public-key}). If
 * either is missing outside a dev-like profile the context fails fast — we
 * never silently fall back to an insecure default.
 *
 * <p>Dev/local/test: when no keys are configured an <em>ephemeral</em> 2048-bit
 * pair is generated at startup so developers can run the service without
 * managing key material. These keys do not survive a restart and are logged
 * with a loud warning.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfig {

    private static final Set<String> DEV_LIKE_PROFILES = Set.of("dev", "local", "test", "default");
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * Immutable holder so the signer (private key) and the verifier (public key)
     * always reference the same pair — critical when keys are generated in-memory.
     */
    public record RsaKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
    }

    @Bean
    public RsaKeyPair rsaKeyPair(JwtProperties properties, Environment environment) {
        String privatePem = normalize(properties.rsa().privateKey());
        String publicPem = normalize(properties.rsa().publicKey());

        if (hasText(privatePem) && hasText(publicPem)) {
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(toStream(privatePem));
            RSAPublicKey publicKey = RsaKeyConverters.x509().convert(toStream(publicPem));
            log.info("Loaded RS256 JWT key pair from configured PEM material.");
            return new RsaKeyPair(publicKey, privateKey);
        }

        if (!isDevLike(environment)) {
            throw new IllegalStateException(
                    "JWT RSA keys are not configured. Set jwt.rsa.private-key and jwt.rsa.public-key "
                            + "(via JWT_PRIVATE_KEY / JWT_PUBLIC_KEY environment variables) before starting "
                            + "outside of dev/local/test profiles.");
        }

        log.warn("============================================================");
        log.warn("No RSA JWT keys configured - generating an EPHEMERAL pair.");
        log.warn("Tokens will NOT survive a restart. NEVER use this in prod.");
        log.warn("============================================================");
        KeyPair generated = generateEphemeralKeyPair();
        return new RsaKeyPair((RSAPublicKey) generated.getPublic(), (RSAPrivateKey) generated.getPrivate());
    }

    private static KeyPair generateEphemeralKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate ephemeral RSA key pair", e);
        }
    }

    private static boolean isDevLike(Environment environment) {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) {
            return true; // no explicit profile == local default
        }
        return Arrays.stream(active).anyMatch(p -> DEV_LIKE_PROFILES.contains(p.toLowerCase()));
    }

    /** Tolerate env vars that carry escaped "\n" instead of real line breaks. */
    private static String normalize(String pem) {
        if (pem == null) {
            return null;
        }
        return pem.replace("\\n", "\n").trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static InputStream toStream(String pem) {
        return new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
    }
}
