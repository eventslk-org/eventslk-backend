package com.event_registration.lk.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Maps the custom {@code roles} claim onto Spring Security authorities.
 *
 * <p>Defensively handles every shape a {@code roles} claim is seen to take in
 * the wild:
 * <ul>
 *   <li>a JSON array — {@code ["ADMIN","USER"]}</li>
 *   <li>a single string — {@code "ADMIN"}</li>
 *   <li>a space- or comma-delimited string — {@code "ADMIN USER"} / {@code "ADMIN,USER"}</li>
 * </ul>
 *
 * <p>Each value is normalised (trimmed, upper-cased) and given the {@code ROLE_}
 * prefix exactly once so {@code hasRole(...)} / {@code @PreAuthorize} work as
 * expected. Blank or null claims yield no authorities rather than throwing —
 * an authenticated token with no roles is a valid (if unprivileged) caller.
 */
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object claim = jwt.getClaim(ROLES_CLAIM);
        if (claim == null) {
            return List.of();
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        if (claim instanceof Collection<?> values) {
            for (Object value : values) {
                addAuthority(authorities, value);
            }
        } else if (claim instanceof String raw) {
            for (String token : raw.split("[,\\s]+")) {
                addAuthority(authorities, token);
            }
        }
        return authorities;
    }

    private void addAuthority(Set<GrantedAuthority> authorities, Object value) {
        if (value == null) {
            return;
        }
        String role = value.toString().trim().toUpperCase(Locale.ROOT);
        if (role.isEmpty()) {
            return;
        }
        if (!role.startsWith(ROLE_PREFIX)) {
            role = ROLE_PREFIX + role;
        }
        authorities.add(new SimpleGrantedAuthority(role));
    }
}
