package com.foodloop.identity.api;

import com.foodloop.identity.application.RegistrationService;
import com.foodloop.identity.domain.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        AppUser user = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserProfileResponse.from(user));
    }
}
