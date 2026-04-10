package com.event_registration.lk.service;

import com.event_registration.lk.dto.User;
import com.event_registration.lk.dto.request.LoginRequest;
import com.event_registration.lk.dto.response.UserResponse;

import java.util.ArrayList;

public interface UserService {
    public UserResponse addUser(User user);
    public UserResponse loginUser(LoginRequest loginRequest);
    public UserResponse removeUser(Long userId);
    public UserResponse updateUser(User user);
    public UserResponse getAllUsers();
    public UserResponse getUserByEmail(String email);
}
