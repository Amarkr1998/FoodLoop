package com.foodloop.identity.api;

import com.foodloop.identity.application.UserProfileService;
import com.foodloop.identity.domain.AppUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
        AppUser user = userProfileService.getById(callerUserId(authentication));
        return UserProfileResponse.from(user);
    }

    @PatchMapping("/api/v1/users/me")
    public UserProfileResponse updateMe(JwtAuthenticationToken authentication, @Valid @RequestBody UpdateProfileRequest request) {
        AppUser user = userProfileService.updateProfile(callerUserId(authentication), request);
        return UserProfileResponse.from(user);
    }

    /**
     * See UserProfileService#becomeVolunteer's Javadoc: the caller must
     * re-authenticate before the granted role appears in a token — this
     * endpoint's 204 confirms the grant happened in Keycloak, not that the
     * caller can act as a volunteer on their current session immediately.
     */
    @PostMapping("/api/v1/users/me/become-volunteer")
    public ResponseEntity<Void> becomeVolunteer(JwtAuthenticationToken authentication) {
        userProfileService.becomeVolunteer(callerUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return UUID.fromString(jwt.getSubject());
    }
}
