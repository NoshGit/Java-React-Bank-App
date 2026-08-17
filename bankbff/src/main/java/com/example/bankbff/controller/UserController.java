package com.example.bankbff.controller;

import com.example.bankbff.dto.UserInfoDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/me")
    public UserInfoDto me(@AuthenticationPrincipal OidcUser principal) {
        List<String> roles = principal.getClaimAsStringList("roles");
        if (roles == null) {
            roles = List.of();
        }
        return new UserInfoDto(
                principal.getSubject(),
                principal.getPreferredUsername(),
                principal.getFullName(),
                roles);
    }
}