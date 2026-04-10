package com.event_registration.lk.config;

import com.event_registration.lk.dto.Role;
import com.event_registration.lk.entity.UserEntity;
import com.event_registration.lk.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class AdminUserInitializer {

    @Bean
    public CommandLineRunner initAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findUserEntityByEmailIgnoreCase("admin123@viago.com") == null) {
                UserEntity admin = UserEntity.builder()
                        .username("admin123")
                        .email("admin123@viago.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                log.info("Default admin user created: admin123@viago.com");
            }
        };
    }
}
