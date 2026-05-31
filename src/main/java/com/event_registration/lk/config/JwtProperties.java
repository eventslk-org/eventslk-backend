package com.event_registration.lk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Strongly-typed JWT configuration.
 *
 * <p>Backed by the {@code jwt.*} namespace and ultimately by environment
 * variables in production (no secrets are hardcoded in YAML). The RSA key
 * material is PEM-encoded:
 * <ul>
 *   <li>{@code jwt.rsa.private-key} – PKCS#8 private key, only required by the
 *       token <em>issuer</em> (this service signs at login).</li>
 *   <li>{@code jwt.rsa.public-key} – X.509/SPKI public key, used by the
 *       resource-server {@code JwtDecoder} to <em>verify</em> signatures.</li>
 * </ul>
 *
 * @param issuer       expected {@code iss} claim; tokens with any other issuer are rejected
 * @param audience     expected {@code aud} claim; tokens not targeting this audience are rejected
 * @param accessTokenTtl lifetime of an issued access token
 * @param clockSkew    leeway applied to {@code exp}/{@code nbf} timestamp validation
 * @param rsa          PEM-encoded RSA key material
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration clockSkew,
        Rsa rsa
) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "event-registration-api";
        }
        if (audience == null || audience.isBlank()) {
            audience = "eventslk-clients";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofHours(1);
        }
        if (clockSkew == null) {
            clockSkew = Duration.ofSeconds(30);
        }
        if (rsa == null) {
            rsa = new Rsa(null, null);
        }
    }

    /**
     * @param privateKey PEM PKCS#8 private key (issuer only); blank in dev triggers an ephemeral key pair
     * @param publicKey  PEM X.509 public key (verifier); blank in dev triggers an ephemeral key pair
     */
    public record Rsa(String privateKey, String publicKey) {
    }
}
