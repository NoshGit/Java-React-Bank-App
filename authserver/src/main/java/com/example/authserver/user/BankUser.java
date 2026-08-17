package com.example.authserver.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A UserDetails implementation that carries the domain attributes the token
 * customizer needs:
 *
 *   subjectId     -- becomes the "sub" claim. For the capstone this is the
 *                    customer_number for an account holder and the staff
 *                    username for a teller.
 *   username      -- the login name. For the capstone it equals subjectId, so a
 *                    customer logs in with their customer_number. Goes into the
 *                    "preferred_username" claim.
 *   fullName      -- the "name" claim.
 *   role          -- a single role: "account_holder" or "teller".
 *   allowedScopes -- the scopes this user may hold. The token customizer narrows
 *                    the granted scope to (authorized intersect allowed).
 *
 * The token customizer pulls these directly off the principal at JWT encoding
 * time, which is why a plain Spring User is not enough.
 */
public class BankUser implements UserDetails {

    private final String username;
    private final String subjectId;
    private final String password;
    private final String role;
    private final String fullName;
    private final Set<String> allowedScopes;

    public BankUser(String username,
                    String subjectId,
                    String password,
                    String role,
                    String fullName,
                    Set<String> allowedScopes) {
        this.username = username;
        this.subjectId = subjectId;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.allowedScopes = allowedScopes;
    }

    public String getSubjectId()          { return subjectId; }
    public String getRole()               { return role; }
    public String getFullName()           { return fullName; }
    public Set<String> getAllowedScopes() { return allowedScopes; }

    // Spring Security uses the ROLE_ prefix so that hasRole('TELLER') matches an
    // authority named ROLE_TELLER. We follow that convention here.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override public String getPassword()              { return password; }
    @Override public String getUsername()              { return username; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
