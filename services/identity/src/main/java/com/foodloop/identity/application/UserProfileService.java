package com.foodloop.identity.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.identity.api.UpdateProfileRequest;
import com.foodloop.identity.domain.AppUser;
import com.foodloop.identity.domain.AppUserRepository;
import com.foodloop.identity.infrastructure.keycloak.KeycloakUserProvisioner;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unlike {@link RegistrationService}, these methods run for an already
 * authenticated caller: {@code TenantFilter} (backend-commons) has already
 * populated {@code TenantContext} from the JWT before this method is
 * invoked, so {@code @Transactional} here is safe — the connection checkout
 * it triggers happens after the tenant is already set.
 */
@Service
public class UserProfileService {

    private static final String VOLUNTEER_ROLE = "VOLUNTEER";

    private final AppUserRepository appUserRepository;
    private final KeycloakUserProvisioner keycloakUserProvisioner;

    public UserProfileService(AppUserRepository appUserRepository, KeycloakUserProvisioner keycloakUserProvisioner) {
        this.appUserRepository = appUserRepository;
        this.keycloakUserProvisioner = keycloakUserProvisioner;
    }

    @Transactional(readOnly = true)
    public AppUser getById(UUID userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "No profile for this account."));
    }

    @Transactional
    public AppUser updateProfile(UUID userId, UpdateProfileRequest request) {
        AppUser user = getById(userId);
        user.updateProfile(request.displayName(), request.phone(), request.locale());
        return appUserRepository.save(user);
    }

    /**
     * Self-service (spec Phase 10): any existing account can opt into the
     * VOLUNTEER realm role, granted directly rather than needing platform
     * admin approval — Pickup's volunteer profile registration is the actual
     * gate on who can act as a volunteer day-to-day (its own creation
     * endpoint re-checks this role), this just grants the credential.
     */
    @Transactional(readOnly = true)
    public void becomeVolunteer(UUID userId) {
        getById(userId); // 404s cleanly if the caller's own profile row is somehow missing
        keycloakUserProvisioner.assignRealmRole(userId, VOLUNTEER_ROLE);
    }
}
