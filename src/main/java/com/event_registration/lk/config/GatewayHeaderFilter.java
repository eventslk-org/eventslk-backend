package com.event_registration.lk.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Trusts the X-User-Name / X-User-Roles headers injected by the API Gateway
 * after it has validated the JWT.
 *
 * This filter replaces the legacy cryptographic JwtFilter that lived inside
 * this service back when it was a monolith. The service must now sit behind
 * the gateway — no caller should be able to reach it directly with these
 * headers spoofed (enforce via network policy / security groups).
 */
@Slf4j
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_NAME  = "X-User-Name";
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String username = request.getHeader(HEADER_USER_NAME);
        String rolesHeader = request.getHeader(HEADER_USER_ROLES);

        if (username != null
                && !username.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authenticated request from gateway: user={} roles={}", username, authorities);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Roles arrive as a comma-separated string ("ADMIN" or "ADMIN,USER").
     * Spring Security expects authorities prefixed with "ROLE_" for {@code hasRole(...)}.
     */
    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
