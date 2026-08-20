package com.foodloop.tenant.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.tenant.application.OrganizationService;
import com.foodloop.tenant.domain.OrgMember;
import com.foodloop.tenant.domain.Organization;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/api/v1/organizations")
    public ResponseEntity<OrganizationResponse> create(
            JwtAuthenticationToken authentication, @Valid @RequestBody CreateOrganizationRequest request) {
        Organization organization = organizationService.createOrganization(
                currentTenantId(), callerUserId(authentication), request.name(), request.type(),
                request.latitude(), request.longitude());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrganizationResponse.from(organization));
    }

    @GetMapping("/api/v1/organizations/{id}")
    public OrganizationResponse get(@PathVariable UUID id) {
        return OrganizationResponse.from(organizationService.getOrganization(id));
    }

    @PatchMapping("/api/v1/organizations/{id}")
    public OrganizationResponse update(
            JwtAuthenticationToken authentication, @PathVariable UUID id, @RequestBody UpdateOrganizationRequest request) {
        Organization organization = organizationService.updateOrganization(
                id, callerUserId(authentication), request.name(), request.latitude(), request.longitude());
        return OrganizationResponse.from(organization);
    }

    /**
     * The Matching Agent's searchNearbyReceivers tool calls this (Phase 7) —
     * same authenticated-boundary pattern as every other agent/business-API
     * call (docs/architecture/05 §1), not a special internal-only route.
     */
    @GetMapping("/api/v1/organizations")
    public Page<OrganizationResponse> searchNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return organizationService
                .searchNearbyReceivers(currentTenantId(), lat, lng, radiusKm * 1000.0, type, pageable)
                .map(OrganizationResponse::from);
    }

    @PostMapping("/api/v1/organizations/{id}/members")
    public ResponseEntity<OrgMemberResponse> addMember(
            JwtAuthenticationToken authentication, @PathVariable UUID id, @Valid @RequestBody AddMemberRequest request) {
        OrgMember member = organizationService.addMember(id, callerUserId(authentication), request.userId(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrgMemberResponse.from(member));
    }

    @GetMapping("/api/v1/organizations/{id}/members")
    public List<OrgMemberResponse> listMembers(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return organizationService.listMembers(id, callerUserId(authentication)).stream()
                .map(OrgMemberResponse::from)
                .toList();
    }

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }

    private UUID currentTenantId() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST,
                    "Request's JWT carried no tenant_id claim.");
        }
        return tenantId;
    }
}
