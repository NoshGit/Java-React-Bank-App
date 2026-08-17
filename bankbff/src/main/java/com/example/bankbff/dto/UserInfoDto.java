package com.example.bankbff.dto;

import java.util.List;

/**
 * Information about the authenticated user, exposed via /api/me.
 *
 * The SPA calls /api/me on load to determine whether the user is logged in
 * and to display the user's name and role. The fields come from the OAuth2
 * authenticated principal and the access token claims.
 *
 *   subject:          the JWT's sub claim. From Lab 2-1 this is the
 *                     customer ID (C001), employee ID (EM01), or auditor ID.
 *   preferredUsername: the login name (alice, edward, audit).
 *   fullName:         the user's display name from the token.
 *   roles:            the user's roles (account_holder, teller, auditor) test.
 */
public record UserInfoDto(
        String subject,
        String preferredUsername,
        String fullName,
        List<String> roles
) {}