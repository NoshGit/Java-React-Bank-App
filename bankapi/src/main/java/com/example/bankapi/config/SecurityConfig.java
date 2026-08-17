package com.example.bankapi.config;

import com.example.bankapi.dto.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Two authorization layers per the API Security Contract:
 *   1. This filter chain -- authentication + coarse role rules.
 *   2. @PreAuthorize plus manual ownership checks in the service layer -- fine-grained
 *      ownership (see AccountService, MoneyMovementService).
 * @EnableMethodSecurity is required for @PreAuthorize to be evaluated at all;
 * the original lab SecurityConfig did not declare it.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customers/*/accounts").hasRole("TELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/accounts/*/status").hasRole("TELLER")
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/deposits").hasRole("TELLER")
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/withdrawals").hasRole("TELLER")
                        .requestMatchers(HttpMethod.GET, "/api/reports/transactions").hasRole("TELLER")
                        .requestMatchers(HttpMethod.GET, "/api/accounts").hasRole("ACCOUNT_HOLDER")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Without this, a 403 from the filter chain's own role rules (e.g. a teller hitting
     * GET /api/accounts) never reaches GlobalExceptionHandler -- it's short-circuited before
     * the DispatcherServlet -- so it comes back as a bare, bodyless 403 while an ownership
     * @PreAuthorize denial (thrown from inside a controller method) gets the JSON error body.
     * This keeps both codepaths producing the same shape.
     */
    private AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return (request, response, ex) -> {
            response.setStatus(403);
            response.setContentType("application/json");
            ErrorResponseDto body = ErrorResponseDto.of(403, "Forbidden", "Not authorized for this resource");
            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }

    /**
     * The Auth Server puts the caller's role in a custom "roles" claim (e.g. "teller"),
     * not the standard "scope" claim. hasRole(...) needs a ROLE_* authority, which the
     * default JwtGrantedAuthoritiesConverter (scope-only) never produces, so this adds
     * ROLE_* authorities from "roles" on top of the default SCOPE_* authorities from "scope".
     */
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        Converter<Jwt, Collection<GrantedAuthority>> rolesConverter = jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.of();
            }
            return roles.stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .toList();
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
            authorities.addAll(rolesConverter.convert(jwt));
            return authorities;
        });
        return converter;
    }
}
