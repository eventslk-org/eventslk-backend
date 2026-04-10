package com.event_registration.lk.controller;

import com.event_registration.lk.dto.User;
import com.event_registration.lk.dto.request.LoginRequest;
import com.event_registration.lk.dto.response.UserResponse;
import com.event_registration.lk.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

//Done
@RestController
@RequestMapping("/auth")
@Slf4j
@CrossOrigin
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signUpUser(@Valid @RequestBody User user){
        UserResponse response = userService.addUser(user);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest){
        log.info("Login attempt for user: {}", loginRequest.getEmail());
        UserResponse response = userService.loginUser(loginRequest);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<String> hello(){
        return ResponseEntity.ok("hello auth");
    }



}
