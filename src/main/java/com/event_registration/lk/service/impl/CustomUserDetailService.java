package com.event_registration.lk.service.impl;

import com.event_registration.lk.entity.UserEntity;
import com.event_registration.lk.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

//For Authentication and Authorization using Spring Security
@Service
public class CustomUserDetailService implements UserDetailsService {

    UserRepository userRepository;

    public CustomUserDetailService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name()) // Assuming enum Role.USER, etc.
                .disabled(!user.isEmailVerified())
                .build();
    }
}
