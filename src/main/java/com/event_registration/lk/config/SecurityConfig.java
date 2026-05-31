package com.event_registration.lk.config;

import com.event_registration.lk.config.JwtKeyConfig.RsaKeyPair;
import com.event_registration.lk.security.JwtRoleConverter;
import com.event_registration.lk.security.ProblemDetailAccessDeniedHandler;
import com.event_registration.lk.security.ProblemDetailAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Stateless security configuration.
 *
 * <p><strong>Primary authentication:</strong> the service is an OAuth2
 * Resource Server. Every request carrying a {@code Bearer} token is verified
 * cryptographically (RS256) by {@link #jwtDecoder} with strict {@code iss} /
 * {@code aud} / timestamp validation, then mapped to authorities by
 * {@link JwtRoleConverter}. Expired or tampered tokens are rejected at the
 * filter layer before any controller runs.
 *
 * <p><strong>Fallback authentication:</strong> when no Bearer token is present,
 * {@link GatewayHeaderFilter} populates the context from gateway-injected
 * {@code X-User-*} headers (trusted only on the private network behind the
 * gateway). It never overrides an identity already established by a verified JWT.
 *
 * <p>Failures are rendered as RFC 7807 problem documents by
 * {@link ProblemDetailAuthenticationEntryPoint} (401) and
 * {@link ProblemDetailAccessDeniedHandler} (403).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final GatewayHeaderFilter gatewayHeaderFilter;
    private final JwtProperties jwtProperties;
    private final RsaKeyPair rsaKeyPair;
    private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

    /** Comma-separated allowed origins; defaults to local dev hosts only (never "*"). */
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(UserDetailsService userDetailsService,
                          GatewayHeaderFilter gatewayHeaderFilter,
                          JwtProperties jwtProperties,
                          RsaKeyPair rsaKeyPair,
                          ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
                          ProblemDetailAccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.gatewayHeaderFilter = gatewayHeaderFilter;
        this.jwtProperties = jwtProperties;
        this.rsaKeyPair = rsaKeyPair;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // stateless API; no cookies/sessions
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/event").permitAll()
                        .requestMatchers("/event/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Fallback only: runs after the bearer filter, populates context
                // from gateway headers when no JWT has already authenticated the request.
                .addFilterAfter(gatewayHeaderFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    /**
     * RS256-only decoder. The signature is verified with the public key; the
     * validator chain enforces timestamp ({@code exp}/{@code nbf} with bounded
     * clock skew), issuer and audience. Anything else is rejected before the
     * authentication token is created.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(rsaKeyPair.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(jwtProperties.clockSkew()),
                new JwtIssuerValidator(jwtProperties.issuer()),
                audienceValidator(jwtProperties.audience()));
        decoder.setJwtValidator(validators);
        return decoder;
    }

    /** Maps the {@code roles} claim to ROLE_-prefixed authorities and uses {@code sub} as the principal name. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(String expectedAudience) {
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The required audience '" + expectedAudience + "' is missing",
                null);
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(expectedAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(error);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
