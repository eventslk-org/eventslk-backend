package com.event_registration.lk.service.impl;

import com.event_registration.lk.config.JwtKeyConfig.RsaKeyPair;
import com.event_registration.lk.config.JwtProperties;
import com.event_registration.lk.dto.Role;
import com.event_registration.lk.dto.request.LoginRequest;
import com.event_registration.lk.entity.UserEntity;
import com.event_registration.lk.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues RS256-signed access tokens.
 *
 * <p>This service is the token <em>issuer</em>: it signs with the RSA private
 * key. Verification is performed independently by the resource-server
 * {@code JwtDecoder} (see {@code SecurityConfig}) using the matching public
 * key, so this class no longer contains any validation logic — that concern now
 * lives entirely at the security filter layer.
 *
 * <p>Emitted claims follow RFC 7519: {@code iss}, {@code aud}, {@code sub},
 * {@code iat}, {@code exp}, a unique {@code jti}, and a {@code roles} array
 * consumed by the authority converter.
 */
@Service
@Slf4j
public class JwtServiceImpl {

    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private final RsaKeyPair rsaKeyPair;

    public JwtServiceImpl(UserRepository userRepository,
                          JwtProperties jwtProperties,
                          RsaKeyPair rsaKeyPair) {
        this.userRepository = userRepository;
        this.jwtProperties = jwtProperties;
        this.rsaKeyPair = rsaKeyPair;
    }

    public String generateJwtToken(LoginRequest loginRequest) {
        String subject = loginRequest.getEmail();

        UserEntity user = userRepository.findUserEntityByEmailIgnoreCase(subject);
        Role role = (user != null && user.getRole() != null) ? user.getRole() : Role.USER;

        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.accessTokenTtl());

        // roles is intentionally a JSON array so additional roles can be added
        // without changing the token shape or the downstream converter.
        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .audience().add(jwtProperties.audience()).and()
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("roles", List.of(role.name()))
                .signWith(rsaKeyPair.privateKey(), Jwts.SIG.RS256)
                .compact();
    }
}
