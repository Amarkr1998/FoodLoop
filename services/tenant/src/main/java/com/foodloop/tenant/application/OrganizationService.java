package com.foodloop.tenant.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.tenant.domain.GeoUtils;
import com.foodloop.tenant.domain.OrgMember;
import com.foodloop.tenant.domain.OrgMemberRepository;
import com.foodloop.tenant.domain.OrgMemberRole;
import com.foodloop.tenant.domain.Organization;
import com.foodloop.tenant.domain.OrganizationRepository;
import com.foodloop.tenant.domain.OrganizationType;
import com.foodloop.tenant.infrastructure.events.OrganizationEventPublisher;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RLS (V1__create_tenant_and_organization.sql) enforces tenant isolation at
 * the database layer; the authorization checks here are a second, narrower
 * layer on top — "is this caller a member/admin of *this specific*
 * organization" — which RLS alone can't express (RLS answers "same tenant
 * or not", not "same organization or not").
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrganizationEventPublisher eventPublisher;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrgMemberRepository orgMemberRepository,
            OrganizationEventPublisher eventPublisher) {
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Organization createOrganization(
            UUID tenantId, UUID callerUserId, String name, OrganizationType type, BigDecimal latitude, BigDecimal longitude) {
        Organization organization = new Organization(tenantId, name, type);
        if (latitude != null && longitude != null) {
            organization.updateLocation(GeoUtils.point(latitude.doubleValue(), longitude.doubleValue()));
        }
        organization = organizationRepository.save(organization);
        orgMemberRepository.save(new OrgMember(tenantId, organization.getId(), callerUserId, OrgMemberRole.ORG_ADMIN));
        eventPublisher.publishOrganizationCreated(organization);
        return organization;
    }

    @Transactional(readOnly = true)
    public Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> notFound(organizationId));
    }

    @Transactional
    public Organization updateOrganization(
            UUID organizationId, UUID callerUserId, String newName, BigDecimal latitude, BigDecimal longitude) {
        requireAdmin(organizationId, callerUserId);
        Organization organization = getOrganization(organizationId);
        organization.rename(newName);
        if (latitude != null && longitude != null) {
            organization.updateLocation(GeoUtils.point(latitude.doubleValue(), longitude.doubleValue()));
        }
        return organizationRepository.save(organization);
    }

    /**
     * Matching Agent's nearby-receiver search (Phase 7, docs/architecture/05
     * §3): only orgs that opted in by setting a location are candidates —
     * donor org types are excluded structurally by
     * {@link OrganizationRepository#searchNearbyReceivers}, not filtered
     * here, so a caller can't accidentally widen the query to donors.
     */
    @Transactional(readOnly = true)
    public Page<Organization> searchNearbyReceivers(
            UUID tenantId, double lat, double lng, double radiusMeters, String type, Pageable pageable) {
        return organizationRepository.searchNearbyReceivers(tenantId, lat, lng, radiusMeters, type, pageable);
    }

    @Transactional
    public OrgMember addMember(UUID organizationId, UUID callerUserId, UUID newMemberUserId, OrgMemberRole role) {
        requireAdmin(organizationId, callerUserId);
        Organization organization = getOrganization(organizationId);
        if (orgMemberRepository.existsByOrganizationIdAndUserId(organizationId, newMemberUserId)) {
            throw new ApiException("ALREADY_A_MEMBER", HttpStatus.CONFLICT, "That user is already a member of this organization.");
        }
        return orgMemberRepository.save(new OrgMember(organization.getTenantId(), organizationId, newMemberUserId, role));
    }

    @Transactional(readOnly = true)
    public List<OrgMember> listMembers(UUID organizationId, UUID callerUserId) {
        requireMember(organizationId, callerUserId);
        return orgMemberRepository.findByOrganizationId(organizationId);
    }

    private void requireAdmin(UUID organizationId, UUID callerUserId) {
        OrgMember member = orgMemberRepository.findByOrganizationIdAndUserId(organizationId, callerUserId)
                .orElseThrow(() -> forbidden(organizationId));
        if (member.getRole() != OrgMemberRole.ORG_ADMIN) {
            throw forbidden(organizationId);
        }
    }

    private void requireMember(UUID organizationId, UUID callerUserId) {
        if (!orgMemberRepository.existsByOrganizationIdAndUserId(organizationId, callerUserId)) {
            throw forbidden(organizationId);
        }
    }

    private ApiException forbidden(UUID organizationId) {
        return new ApiException("NOT_ORGANIZATION_MEMBER", HttpStatus.FORBIDDEN,
                "You are not authorized to act on organization " + organizationId + ".");
    }

    private ApiException notFound(UUID organizationId) {
        return new ApiException("ORGANIZATION_NOT_FOUND", HttpStatus.NOT_FOUND,
                "No organization found with id " + organizationId + ".");
    }
}
