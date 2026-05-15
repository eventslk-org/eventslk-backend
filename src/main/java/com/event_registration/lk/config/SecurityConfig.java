package com.event_registration.lk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//Authenticate and set authorization rules
//provide password encoder instances,
//configure stateless session management,
/* */
// @Configuration
// @EnableWebSecurity
// public class SecurityConfig {

//     private UserDetailsService userDetailsService;
//     private JwtFilter jwtFilter;

//     public SecurityConfig(UserDetailsService userDetailsService, JwtFilter jwtFilter) {
//         this.userDetailsService = userDetailsService;
//         this.jwtFilter = jwtFilter;
//     }

//     @Bean
//     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//         return http
//                 .cors(cors -> cors.configurationSource(request -> {
//                     var config = new org.springframework.web.cors.CorsConfiguration();
//                     config.setAllowedOrigins(java.util.List.of("*"));
//                     config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//                     config.setAllowedHeaders(java.util.List.of("*"));
//                     config.setExposedHeaders(java.util.List.of("Authorization"));
//                     config.setAllowCredentials(false);
//                     return config;
//                 }))
//                 .csrf(csrf -> csrf.disable())
//                 .authorizeHttpRequests(auth -> auth
//                         .requestMatchers("/auth/**").permitAll()
//                         .requestMatchers(HttpMethod.GET, "/event").permitAll()
//                         .requestMatchers("/event/**").hasRole("ADMIN")
//                         .requestMatchers("/admin/**").hasRole("ADMIN")
//                         .anyRequest().authenticated())
//                 .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                 .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
//                 .build();
//     }

//     @Configuration
//     public class WebConfig implements WebMvcConfigurer {
//         @Override
//         public void addCorsMappings(CorsRegistry registry) {
//             registry.addMapping("/**")
//                     .allowedOrigins("*") // later change to specific origins like "http://localhost:3000"
//                     .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                     .allowedHeaders("*");
//         }
//     }

//     // @Bean
//     // public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//     // return http
//     // .cors(cors -> cors
//     // .configurationSource(request -> {
//     // var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
//     // corsConfiguration.setAllowedOriginPatterns(
//     // java.util.List.of("http://localhost:[*]", "https://localhost:[*]"));
//     // corsConfiguration
//     // .setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE",
//     // "OPTIONS"));
//     // corsConfiguration.setAllowedHeaders(java.util.List.of("*"));
//     // corsConfiguration.setExposedHeaders(java.util.List.of("Authorization",
//     // "Content-Type"));
//     // corsConfiguration.setAllowCredentials(true);
//     // return corsConfiguration;
//     // }))
//     // .csrf(customizer -> customizer.disable())
//     // .authorizeHttpRequests(request -> request
//     // .requestMatchers("/auth/**").permitAll()
//     // .requestMatchers(HttpMethod.GET, "/event").permitAll() // public event
//     // listing for
//     // // client-frontend
//     // .requestMatchers("/event/**").hasRole("ADMIN")
//     // .requestMatchers("/book/**").hasRole("USER")
//     // .requestMatchers("/admin/**").hasRole("ADMIN")
//     // .requestMatchers("/user/get-all-users",
//     // "/user/get-user-by-email").hasRole("ADMIN")
//     // .anyRequest().authenticated())
//     // .httpBasic(Customizer.withDefaults())
//     // .sessionManagement(session ->
//     // session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//     // .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
//     // .build();
//     // }

//     // Total auth security disable option
//     /*
//      * @Bean
//      * public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//      * //generated for disable entire auth
//      * return http
//      * .csrf(csrf -> csrf.disable()) // Disable CSRF for APIs
//      * .authorizeHttpRequests(auth -> auth
//      * .anyRequest().permitAll() // 🔓 Allow all requests
//      * )
//      * .build(); // No HTTP basic, no session
//      * }
//      */

//     @Bean
//     public PasswordEncoder passwordEncoder() {
//         return new BCryptPasswordEncoder();
//     }

//     @Bean
//     public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
//             throws Exception {
//         return authenticationConfiguration.getAuthenticationManager();
//     }

//     @Bean
//     public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
//         DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
//         provider.setPasswordEncoder(new BCryptPasswordEncoder());
//         provider.setUserDetailsService(userDetailsService);

//         return provider;
//     }
// }


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtFilter jwtFilter;

    public SecurityConfig(UserDetailsService userDetailsService, JwtFilter jwtFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 1. Unified CORS configuration
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.List.of("*")); 
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setExposedHeaders(java.util.List.of("Authorization"));
                    config.setAllowCredentials(false);
                    return config;
                }))
                // 2. Disable CSRF for stateless APIs
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/event").permitAll()
                        .requestMatchers("/event/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // 4. Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // 5. Remove the inner WebConfig class to avoid conflict. 
    // The FilterChain CORS is sufficient for Spring Security.

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}