package com.example.bankbff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        // Use local redirect to the app sign-in flow and also trigger provider logout so SSO session is cleared.

        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        // redirect to frontend after provider logout — must match authserver RegisteredClient postLogoutRedirectUri
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("http://localhost:5173/logged-out");

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/**", "/logout").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())

                // Dual-audience entry point (Lab 4.7 Patch 1) -- unchanged.
                .exceptionHandling(ex -> {
                    MediaTypeRequestMatcher jsonMatcher =
                            new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON);
                    jsonMatcher.setUseEquals(true);
                    ex
                            .defaultAuthenticationEntryPointFor(
                                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                    jsonMatcher)
                            .defaultAuthenticationEntryPointFor(
                                    new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/bank-auth"),
                                    AnyRequestMatcher.INSTANCE);
                })

                .oauth2Login(Customizer.withDefaults())

                .logout(logout -> logout
                    .logoutRequestMatcher(request -> "GET".equalsIgnoreCase(request.getMethod()) && "/logout".equals(request.getServletPath()))
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("BFF_SESSION")
                    .logoutSuccessHandler(oidcLogoutSuccessHandler)
                    .permitAll()
                )

                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}