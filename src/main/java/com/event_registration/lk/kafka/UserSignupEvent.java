package com.event_registration.lk.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSignupEvent {
    private String email;
    private String username;
    private String verificationToken;
    private String verificationLink;
}
