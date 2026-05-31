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
 * <strong>Fallback</strong> authentication from the {@code X-User-Name} /
 * {@code X-User-Roles} headers injected by the API Gateway after it has
 * validated the JWT.
 *
 * <p>This now runs <em>after</em> the resource-server {@code BearerTokenAuthenticationFilter}
 * and is strictly secondary to cryptographic JWT validation:
 * <ul>
 *   <li>If a request carries an {@code Authorization: Bearer} header it is left
 *       entirely to the resource server — header trust is skipped so a spoofed
 *       {@code X-User-*} header can never shadow or supplement a real token.</li>
 *   <li>It only populates the context when no authentication is already present.</li>
 * </ul>
 *
 * <p>These headers carry no cryptographic proof, so this path is only safe when
 * the service is unreachable except through the gateway. Enforce that with
 * network policy / security groups; the gateway must also strip any inbound
 * {@code X-User-*} headers from clients.
 */
@Slf4j
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_NAME  = "X-User-Name";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        boolean bearerPresent = authorization != null && authorization.startsWith(BEARER_PREFIX);

        String username = request.getHeader(HEADER_USER_NAME);
        String rolesHeader = request.getHeader(HEADER_USER_ROLES);

        if (!bearerPresent
                && username != null
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
