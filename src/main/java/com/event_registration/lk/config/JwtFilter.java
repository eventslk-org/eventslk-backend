package com.event_registration.lk.config;

import com.event_registration.lk.service.UserService;
import com.event_registration.lk.service.impl.CustomUserDetailService;
import com.event_registration.lk.service.impl.JwtServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//Intercept every http request and validate jwt tokens and authenticate users
@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private ApplicationContext applicationContext;
    private JwtServiceImpl jwtService;

    public JwtFilter(JwtServiceImpl jwtService, ApplicationContext applicationContext) {
        this.jwtService = jwtService;
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")){
                token = authHeader.substring(7);
                email = jwtService.extractEmail(token);
            }
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null){
                UserDetails userDetails = applicationContext.getBean(CustomUserDetailService.class).loadUserByUsername(email);

                if(jwtService.validateToken(token,userDetails)){
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("JWT token is invalid or expired: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
        }
        
        filterChain.doFilter(request,response);
    }
}
