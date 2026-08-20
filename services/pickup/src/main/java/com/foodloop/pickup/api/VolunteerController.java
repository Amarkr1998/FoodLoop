package com.foodloop.pickup.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.pickup.application.VolunteerService;
import com.foodloop.pickup.domain.VolunteerProfile;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VolunteerController {

    private final VolunteerService volunteerService;

    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    /**
     * Requires the caller's token to already carry the VOLUNTEER realm role
     * (granted by Identity's {@code POST /api/v1/users/me/become-volunteer})
     * — same {@code realm_access.roles} check as Food's requireTrustOpsCaller,
     * the second use of that trust primitive in this codebase.
     */
    @PostMapping("/api/v1/volunteers")
    public ResponseEntity<VolunteerProfileResponse> register(
            JwtAuthenticationToken authentication, @Valid @RequestBody RegisterVolunteerRequest request) {
        requireVolunteerRole(authentication);
        VolunteerProfile profile = volunteerService.register(
                tenantId(authentication), callerUserId(authentication), request.vehicleType(), request.capacityServings());
        return ResponseEntity.status(HttpStatus.CREATED).body(VolunteerProfileResponse.from(profile));
    }

    @GetMapping("/api/v1/volunteers/me")
    public VolunteerProfileResponse me(JwtAuthenticationToken authentication) {
        return VolunteerProfileResponse.from(volunteerService.getByUserId(callerUserId(authentication)));
    }

    @PatchMapping("/api/v1/volunteers/me/availability")
    public VolunteerProfileResponse updateAvailability(
            JwtAuthenticationToken authentication, @Valid @RequestBody UpdateAvailabilityRequest request) {
        return VolunteerProfileResponse.from(
                volunteerService.updateAvailability(callerUserId(authentication), request.available()));
    }

    @PatchMapping("/api/v1/volunteers/me/location")
    public VolunteerProfileResponse updateLocation(
            JwtAuthenticationToken authentication, @Valid @RequestBody UpdateLocationRequest request) {
        return VolunteerProfileResponse.from(volunteerService.updateLocation(
                callerUserId(authentication), request.latitude().doubleValue(), request.longitude().doubleValue()));
    }

    private void requireVolunteerRole(JwtAuthenticationToken authentication) {
        Object realmAccess = authentication.getToken().getClaims().get("realm_access");
        List<String> roles = (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof List<?> rawRoles)
                ? rawRoles.stream().map(String::valueOf).toList()
                : List.of();
        if (!roles.contains("VOLUNTEER")) {
            throw new ApiException("VOLUNTEER_ROLE_REQUIRED", HttpStatus.FORBIDDEN,
                    "Grant yourself the VOLUNTEER role first (POST /api/v1/users/me/become-volunteer), "
                            + "then sign in again before registering a volunteer profile.");
        }
    }

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }

    private UUID tenantId(JwtAuthenticationToken authentication) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return tenantId;
    }
}
