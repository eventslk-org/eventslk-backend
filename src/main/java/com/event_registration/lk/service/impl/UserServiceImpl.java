package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.User;
import com.event_registration.lk.dto.request.LoginRequest;
import com.event_registration.lk.dto.response.UserResponse;
import com.event_registration.lk.entity.UserEntity;
import com.event_registration.lk.repository.UserRepository;
import com.event_registration.lk.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    ObjectMapper objectMapper;
    AuthenticationManager authenticationManager;
    JwtServiceImpl jwtService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtServiceImpl jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper();
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // Done
    @Override
    public UserResponse addUser(User user) {

        UserEntity userEntity = UserEntity.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .password(passwordEncoder.encode(user.getPassword()))
                .role(user.getRole())
                .build();
        if (userRepository.existsByEmailContainingIgnoreCase(user.getEmail())) {
            return new UserResponse("signup", "email already in use");
        }
        try {
            userRepository.save(userEntity);

            // send email using producer
            // signupEventProducer.publishUserSignupEvent(userEntity.getEmail());

            return new UserResponse("signup", "success");
        } catch (Exception e) {
            return new UserResponse("signup", "unsuccess");
        }
    }

    // Done
    @Override
    public UserResponse loginUser(LoginRequest loginRequest) {

        UserEntity entity = userRepository.findUserEntityByEmailIgnoreCase(loginRequest.getEmail());

        if (entity == null) {
            return new UserResponse("login", "user not found");
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
}
