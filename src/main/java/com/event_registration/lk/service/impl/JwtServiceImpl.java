package com.event_registration.lk.service.impl;

import com.event_registration.lk.dto.request.LoginRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtServiceImpl {

//    private Key SECRET_KEY;
//
//    public JwtServiceImpl() {
//        try {
//            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
//            SecretKey secretKey = keyGenerator.generateKey();
//            String stingKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
//            byte[] decode = Decoders.BASE64.decode(stingKey);
//            SECRET_KEY = Keys.hmacShaKeyFor(decode);
//        }catch (Exception e){
//            log.warn("error in generating secret key");
//        }
//
//    }
//
//    public String generateJwtToken(User user) {
//        Map<String, Object> claims = new HashMap<>();
//        return Jwts.builder()
//                .claims()
//                .add(claims)
//                .subject(user.getUsername())
//                .issuedAt(new Date(System.currentTimeMillis()))
//                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
//                .and()
//                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getEncoded()))
//                .compact();
//    }

    @Value("${jwt.secret}")
    private String SECRET;

    public JwtServiceImpl() {
        // Secret is injected via @Value
    }
    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateJwtToken(LoginRequest loginRequest) {
        // String subject = loginRequest.getEmail() != null ? loginRequest.getEmail() : loginRequest.getEmail();
        String subject = loginRequest.getEmail();
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour
                .and()
                .signWith(getSecretKey())
                .compact();
    }

    public String extractEmail(String token){
        return extractClaim(token, Claims::getSubject);
    }
    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractEmail(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
