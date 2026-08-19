package com.foodloop.identity.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.identity.api.UpdateProfileRequest;
import com.foodloop.identity.domain.AppUser;
import com.foodloop.identity.domain.AppUserRepository;
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

    private final AppUserRepository appUserRepository;

    public UserProfileService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public AppUser getByKeycloakId(UUID keycloakId) {
        return appUserRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "No profile for this account."));
    }

    @Transactional
    public AppUser updateProfile(UUID keycloakId, UpdateProfileRequest request) {
        AppUser user = getByKeycloakId(keycloakId);
        user.updateProfile(request.displayName(), request.phone(), request.locale());
        return appUserRepository.save(user);
    }
}
