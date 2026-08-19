package com.foodloop.tenant.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.tenant.application.OrganizationService;
import com.foodloop.tenant.domain.OrgMember;
import com.foodloop.tenant.domain.Organization;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
                currentTenantId(), callerUserId(authentication), request.name(), request.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrganizationResponse.from(organization));
    }

    @GetMapping("/api/v1/organizations/{id}")
    public OrganizationResponse get(@PathVariable UUID id) {
        return OrganizationResponse.from(organizationService.getOrganization(id));
    }

    @PatchMapping("/api/v1/organizations/{id}")
    public OrganizationResponse update(
            JwtAuthenticationToken authentication, @PathVariable UUID id, @RequestBody UpdateOrganizationRequest request) {
        Organization organization = organizationService.renameOrganization(id, callerUserId(authentication), request.name());
        return OrganizationResponse.from(organization);
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
