package com.example.authserver.config;

import com.example.authserver.user.BankUser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class AuthorizationServerConfig {

    // ---- Scope sets per role. Auditor is dropped for the capstone. ----
    private static final Set<String> ACCOUNT_HOLDER_SCOPES = Set.of(
            "account.read", "account.write",
            "transaction.read", "transaction.create",
            "customer.read");

    private static final Set<String> TELLER_SCOPES = Stream.concat(
                    ACCOUNT_HOLDER_SCOPES.stream(),
                    Stream.of("account.create", "customer.write"))
            .collect(Collectors.toUnmodifiableSet());

    // ===== Filter chain 1: the OAuth2 / OIDC protocol endpoints =====
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());
        http.exceptionHandling(e ->
                e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));
        http.oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // ===== Filter chain 2: the login form for users =====
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    // ===== Password encoder =====
 /*   @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
*/


    @Bean
    public PasswordEncoder passwordEncoder(){
        Map<String, PasswordEncoder> encoders = Map.of("noop", NoOpPasswordEncoder.getInstance(), "bcrypt", new BCryptPasswordEncoder());
        DelegatingPasswordEncoder delegatingPasswordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        return delegatingPasswordEncoder;
    }
    // ===== Capstone users =====
    // Account holders log in with their customer_number (which is also the sub).
    // The teller logs in with a staff username. All share the password "password",
    // which is acceptable for a lab and ONLY for a lab.
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {

        BankUser alice = new BankUser(
                "487-978493", "487-978493", encoder.encode("password"),
                "account_holder", "Alice Eke", ACCOUNT_HOLDER_SCOPES);

        BankUser bob = new BankUser(
                "500-100200", "500-100200", encoder.encode("password"),
                "account_holder", "Bob Banala", ACCOUNT_HOLDER_SCOPES);

        BankUser chris = new BankUser(
                "442-100650", "442-100650", encoder.encode("password"),
                "account_holder", "Chris Malik", ACCOUNT_HOLDER_SCOPES);

        BankUser doug = new BankUser(
                "980-104100", "980-104100", encoder.encode("password"),
                "account_holder", "Doug Frenzel", ACCOUNT_HOLDER_SCOPES);

        BankUser teller1 = new BankUser(
                "teller1", "teller1", encoder.encode("password"),
                "teller", "Teller One", TELLER_SCOPES);

        Map<String, BankUser> users = Stream.of(alice, bob, chris, doug, teller1)
                .collect(Collectors.toMap(BankUser::getUsername, u -> u));

        return username -> {
            BankUser user = users.get(username);
            if (user == null) {
                throw new UsernameNotFoundException("Unknown user: " + username);
            }
            return user;
        };
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        RegisteredClient bankClientBff = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("bank-client-bff")
                .clientSecret("{noop}bank-client-bff-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // Login redirect URIs (Lab 4.6 and 4.7).
                .redirectUri("http://localhost:8080/login/oauth2/code/bank-auth")
                .redirectUri("http://localhost:5173/login/oauth2/code/bank-auth")
                // Post-logout redirect URI (Lab 4.8). Must match the BFF's
                // setPostLogoutRedirectUri(...) character for character.
                // Allow post-logout return to the dev UI. The OIDC RP-initiated logout handler will request
                // this URL; the IdP must accept it (exact match).
                .postLogoutRedirectUri("http://localhost:5173/logged-out")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("account.read")
                .scope("account.write")
                .scope("account.create")
                .scope("transaction.read")
                .scope("transaction.create")
                .scope("customer.read")
                .scope("customer.write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(60))
                        .refreshTokenTimeToLive(Duration.ofDays(1))
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(bankClientBff);
    }

    // ===== Token customizer =====
    // For user-bearing access/id tokens: set sub to the domain id, add the OIDC
    // display claims, add a roles claim, and narrow scope to (authorized intersect
    // allowed) while always keeping openid and profile.
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            boolean accessToken = OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType());
            boolean idToken = OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
            if (!accessToken && !idToken) {
                return;
            }
            Authentication principal = context.getPrincipal();
            if (principal.getPrincipal() instanceof BankUser user) {

                context.getClaims().subject(user.getSubjectId());
                context.getClaims().claim("preferred_username", user.getUsername());
                context.getClaims().claim("name", user.getFullName());
                context.getClaims().claim("roles", List.of(user.getRole()));

                if (accessToken) {
                    Set<String> granted = context.getAuthorizedScopes().stream()
                            .filter(s -> user.getAllowedScopes().contains(s)
                                    || OidcScopes.OPENID.equals(s)
                                    || OidcScopes.PROFILE.equals(s))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    context.getClaims().claim("scope", String.join(" ", granted));
                }
            }
        };
    }

    // ===== RSA signing key and JWKS =====
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate RSA key", ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    // The issuer is set in application.yml as http://127.0.0.1:9000.
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
