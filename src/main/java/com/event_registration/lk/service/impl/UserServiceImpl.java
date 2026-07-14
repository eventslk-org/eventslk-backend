package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.Role;
import com.event_registration.lk.dto.User;
import com.event_registration.lk.dto.request.LoginRequest;
import com.event_registration.lk.dto.response.UserResponse;
import com.event_registration.lk.entity.OutboxEvent;
import com.event_registration.lk.entity.UserEntity;
import com.event_registration.lk.kafka.NotificationProducer;
import com.event_registration.lk.kafka.UserSignupEvent;
import com.event_registration.lk.repository.OutboxEventRepository;
import com.event_registration.lk.repository.UserRepository;
import com.event_registration.lk.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    OutboxEventRepository outboxEventRepository;
    PasswordEncoder passwordEncoder;
    ObjectMapper objectMapper;
    AuthenticationManager authenticationManager;
    JwtServiceImpl jwtService;
    NotificationProducer notificationProducer;

    @Value("${app.base-url:http://localhost:8081}")
    private String appBaseUrl;

    @Value("${app.verification.token-ttl-hours:24}")
    private long verificationTokenTtlHours;

    public UserServiceImpl(UserRepository userRepository,
            OutboxEventRepository outboxEventRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtServiceImpl jwtService,
            NotificationProducer notificationProducer) {
        this.userRepository = userRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.notificationProducer = notificationProducer;
    }

    @Override
    @Transactional
    public UserResponse addUser(User user) {

        if (userRepository.existsByEmailContainingIgnoreCase(user.getEmail())) {
            return new UserResponse("signup", "email already in use");
        }

        String verificationToken = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(verificationTokenTtlHours, ChronoUnit.HOURS);

        UserEntity userEntity = UserEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password(passwordEncoder.encode(user.getPassword()))
                // Never trust a client-supplied role: public signup always creates a
                // plain USER. Admins are provisioned via AdminUserInitializer only.
                .role(Role.USER)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiresAt(expiresAt)
                .build();

        try {
            UserEntity savedUser = userRepository.save(userEntity);

            UserSignupEvent signupEvent = UserSignupEvent.builder()
                    .email(savedUser.getEmail())
                    .username(savedUser.getUsername())
                    .verificationToken(verificationToken)
                    .verificationLink(appBaseUrl + "/auth/verify-email?token=" + verificationToken)
                    .build();

            String payload = objectMapper.writeValueAsString(signupEvent);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("User")
                    .aggregateId(String.valueOf(savedUser.getUserId()))
                    .type("UserSignupEvent")
                    .payload(payload)
                    .status(OutboxEvent.Status.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);

            return new UserResponse("signup", "success");
        } catch (JsonProcessingException e) {
            // Force rollback of user insert if we can't serialize the event.
            throw new RuntimeException("Failed to serialize UserSignupEvent for outbox", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register user", e);
        }
    }

    // Done
    @Override
    public UserResponse loginUser(LoginRequest loginRequest) {

        UserEntity entity = userRepository.findUserEntityByEmailIgnoreCase(loginRequest.getEmail());

        if (entity == null) {
            return new UserResponse("login", "user not found");
        }

        if (!entity.isEmailVerified()) {
            return new UserResponse("login", "email not verified");
        }

        User user = objectMapper.convertValue(entity, User.class);

        user.setPassword(null);
        String jwtToken = jwtService.generateJwtToken(loginRequest);
        log.info("login user service layer");
        Authentication authentication = authenticationManager.authenticate(

                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail() /* != null ? user.getEmail() : user.getUsername() */,
                        loginRequest.getPassword()));
        if (authentication.isAuthenticated()) {
            return new UserResponse("login", "success", user, jwtToken);
        } else {
            return new UserResponse("login", "unsuccess");
        }
    }

    // Done
    @Override
    public UserResponse removeUser(Long userId) {
        if (!userRepository.existsByUserId(userId)) {
            return new UserResponse("account-remove", "user not exists");
        }
        try {
            userRepository.deleteById(userId);
            return new UserResponse("account-remove", "success");
        } catch (Exception e) {
            return new UserResponse("account-remove", "error occurred : " + e.getMessage());
        }
    }

    // Done
    @Override
    public UserResponse updateUser(User user) {
        Optional<UserEntity> existingUserEntity = userRepository.findByEmailIgnoreCase(user.getEmail());
        if (existingUserEntity.isEmpty()) {
            return new UserResponse("account-update", "user not exists");
        }
        UserEntity userEntity = objectMapper.convertValue(existingUserEntity.get(), UserEntity.class);
        userEntity.setEmail(user.getEmail());
        userEntity.setUsername(user.getUsername());
        userEntity.setPassword(passwordEncoder.encode(user.getPassword()));

        try {
            userRepository.save(userEntity);
            return new UserResponse("account-update", "success");
        } catch (Exception e) {
            return new UserResponse("account-update", "unsuccess");
        }
    }

    // Done
    @Override
    public UserResponse getAllUsers() {
        ArrayList<User> userList = new ArrayList<>();

        Iterable<UserEntity> userIterable = userRepository.findAll();
        Iterator<UserEntity> userEntityIterator = userIterable.iterator();
        while (userEntityIterator.hasNext()) {
            User user = objectMapper.convertValue(userEntityIterator.next(), User.class);
            userList.add(user);
        }
        return new UserResponse("event-list", "success", userList);
    }

    // Done
    @Override
    public UserResponse getUserByEmail(String email) {
        UserEntity entity = userRepository.findUserEntityByEmailIgnoreCase(email);
        User user = objectMapper.convertValue(entity, User.class);
        user.setPassword(null);
        return new UserResponse("get user by email", "success", user);
    }

    @Override
    @Transactional
    public UserResponse verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            return new UserResponse("verify-email", "invalid token");
        }

        Optional<UserEntity> match = userRepository.findByVerificationToken(token);
        if (match.isEmpty()) {
            return new UserResponse("verify-email", "invalid token");
        }

        UserEntity entity = match.get();

        if (entity.isEmailVerified()) {
            return new UserResponse("verify-email", "already verified");
        }

        Instant expiresAt = entity.getVerificationTokenExpiresAt();
        if (expiresAt == null || Instant.now().isAfter(expiresAt)) {
            return new UserResponse("verify-email", "token expired");
        }

        entity.setEmailVerified(true);
        entity.setEmailVerifiedAt(Instant.now());
        entity.setVerificationToken(null);
        entity.setVerificationTokenExpiresAt(null);
        userRepository.save(entity);

        return new UserResponse("verify-email", "success");
    }

    @Override
    @Transactional
    public UserResponse resendVerification(String email) {
        // Generic response — do not leak whether the email is registered.
        UserResponse generic = new UserResponse("resend-verification",
                "if the account exists and is unverified, a new email has been sent");

        if (email == null || email.isBlank()) {
            return generic;
        }

        Optional<UserEntity> match = userRepository.findByEmailIgnoreCase(email);
        if (match.isEmpty()) {
            return generic;
        }

        UserEntity entity = match.get();
        if (entity.isEmailVerified()) {
            return generic;
        }

        String newToken = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(verificationTokenTtlHours, ChronoUnit.HOURS);
        entity.setVerificationToken(newToken);
        entity.setVerificationTokenExpiresAt(expiresAt);
        userRepository.save(entity);

        try {
            UserSignupEvent signupEvent = UserSignupEvent.builder()
                    .email(entity.getEmail())
                    .username(entity.getUsername())
                    .verificationToken(newToken)
                    .verificationLink(appBaseUrl + "/auth/verify-email?token=" + newToken)
                    .build();

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("User")
                    .aggregateId(String.valueOf(entity.getUserId()))
                    .type("UserSignupEvent")
                    .payload(objectMapper.writeValueAsString(signupEvent))
                    .status(OutboxEvent.Status.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize UserSignupEvent for outbox", e);
        }

        return generic;
    }
}
