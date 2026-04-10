package com.event_registration.lk.controller;

import com.event_registration.lk.dto.User;
import com.event_registration.lk.dto.response.UserResponse;
import com.event_registration.lk.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

import java.util.Date;
//Done
@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable Long userId) {
        UserResponse response = userService.removeUser(userId);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody User user) {
        // Enforce the path variable matches the body if applicable, though the service uses the email in body for finding user.
        // Assuming user.setEmail is used to find.
        UserResponse response = userService.updateUser(user);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    //for testing purpose
    @GetMapping("/hello")
    public ResponseEntity<String> sayHello(){
        Date date = new Date(new java.util.Date().getTime());
        return ResponseEntity.ok("Hello "+date);
    }

    @GetMapping
    public ResponseEntity<UserResponse> getAllUsers(){
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email){
        UserResponse response = userService.getUserByEmail(email);
        if ("success".equals(response.getMessage())) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

}
