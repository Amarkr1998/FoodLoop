package com.foodloop.pickup.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.pickup.application.PickupService;
import com.foodloop.pickup.domain.PickupTask;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The volunteer-mediated actions below (claim/en-route/arrived/unassign)
 * gate on {@link com.foodloop.pickup.domain.VolunteerProfile} existing for
 * the caller, not a fresh JWT realm-role check: a role granted mid-session
 * doesn't appear in an already-issued token (see Identity's
 * KeycloakUserProvisioner#assignRealmRole Javadoc), so re-checking it here
 * would incorrectly reject a volunteer who registered a profile and is
 * still using the token from before they became one — the profile row
 * itself, gated by that role check at registration time
 * (VolunteerService#register), is the durable record of eligibility.
 */
@RestController
public class PickupTaskController {

    private final PickupService pickupService;

    public PickupTaskController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @GetMapping("/api/v1/pickups/{id}")
    public PickupTaskResponse get(@PathVariable UUID id) {
        return PickupTaskResponse.from(pickupService.get(id));
    }

    @PostMapping("/api/v1/pickups/{id}/complete")
    public PickupTaskResponse complete(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        PickupTask task = pickupService.complete(id, callerUserId(authentication));
        return PickupTaskResponse.from(task);
    }

    @PostMapping("/api/v1/pickups/{id}/report-no-show")
    public PickupTaskResponse reportNoShow(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        PickupTask task = pickupService.reportNoShow(id, callerUserId(authentication));
        return PickupTaskResponse.from(task);
    }

    @PostMapping("/api/v1/pickups/{id}/request-volunteer")
    public PickupTaskResponse requestVolunteer(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return PickupTaskResponse.from(pickupService.requestVolunteer(id, callerUserId(authentication)));
    }

    @PostMapping("/api/v1/pickups/{id}/claim")
    public PickupTaskResponse claim(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return PickupTaskResponse.from(pickupService.claimAsVolunteer(id, callerUserId(authentication)));
    }

    @PostMapping("/api/v1/pickups/{id}/en-route")
    public PickupTaskResponse enRoute(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return PickupTaskResponse.from(pickupService.volunteerEnRoute(id, callerUserId(authentication)));
    }

    @PostMapping("/api/v1/pickups/{id}/arrived")
    public PickupTaskResponse arrived(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return PickupTaskResponse.from(pickupService.volunteerArrived(id, callerUserId(authentication)));
    }

    @PostMapping("/api/v1/pickups/{id}/unassign")
    public PickupTaskResponse unassign(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return PickupTaskResponse.from(pickupService.unassignVolunteer(id, callerUserId(authentication)));
    }

    @GetMapping("/api/v1/pickups/available")
    public Page<PickupTaskResponse> available(
            JwtAuthenticationToken authentication,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return pickupService.searchAvailableForVolunteers(tenantId(authentication), lat, lng, radiusKm, pageable)
                .map(PickupTaskResponse::from);
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
