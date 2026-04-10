package com.event_registration.lk.dto.response;

import com.event_registration.lk.dto.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String status;
    private String message;
    private User user;
    private String jwtToken;
    private ArrayList<User> userList;

    public UserResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public UserResponse(String status, String message, User user, String jwtToken) {
        this.status = status;
        this.message = message;
        this.user = user;
        this.jwtToken = jwtToken;
    }

    public UserResponse(String status, String message, String jwtToken) {
        this.status = status;
        this.message = message;
        this.jwtToken = jwtToken;
    }

    public UserResponse(String status, String message, ArrayList<User> userList) {
        this.status = status;
        this.message = message;
        this.userList = userList;
    }

    public UserResponse(String status, String message, User user) {
        this.status = status;
        this.message = message;
        this.user = user;
    }
}
