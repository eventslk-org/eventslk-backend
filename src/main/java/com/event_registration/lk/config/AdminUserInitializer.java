package com.event_registration.lk.config;

import com.event_registration.lk.dto.Role;
import com.event_registration.lk.entity.UserEntity;
import com.event_registration.lk.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * Optionally bootstraps a single administrator account.
 *
 * <p>Credentials are supplied exclusively via configuration (environment
 * variables {@code APP_ADMIN_EMAIL} / {@code APP_ADMIN_PASSWORD}). There are no
 * hardcoded defaults: if either value is absent the bootstrap is skipped, so a
 * well-known admin/password pair can never be seeded by accident. The password
 * is BCrypt-hashed before persistence and the existing account is never
 * overwritten.
 */
@Configuration
@Slf4j
public class AdminUserInitializer {

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                log.info("Admin bootstrap skipped: APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD not set.");
                return;
            }

            if (userRepository.findUserEntityByEmailIgnoreCase(adminEmail) != null) {
                log.info("Admin bootstrap skipped: account already exists for {}", adminEmail);
                return;
            }

            UserEntity admin = UserEntity.builder()
                    .username(adminEmail.substring(0, adminEmail.indexOf('@') > 0 ? adminEmail.indexOf('@') : adminEmail.length()))
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .emailVerified(true)
                    .emailVerifiedAt(Instant.now())
                    .build();
            userRepository.save(admin);
            log.info("Bootstrapped admin account for {}", adminEmail);
        };
    }
}
