package com.foodloop.identity.api;

import com.foodloop.identity.application.UserProfileService;
import com.foodloop.identity.domain.AppUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/api/v1/users/me")
    public UserProfileResponse me(JwtAuthenticationToken authentication) {
        AppUser user = userProfileService.getByKeycloakId(keycloakId(authentication));
        return UserProfileResponse.from(user);
    }

    @PatchMapping("/api/v1/users/me")
    public UserProfileResponse updateMe(JwtAuthenticationToken authentication, @Valid @RequestBody UpdateProfileRequest request) {
        AppUser user = userProfileService.updateProfile(keycloakId(authentication), request);
        return UserProfileResponse.from(user);
    }

    private UUID keycloakId(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return UUID.fromString(jwt.getSubject());
    }
}
