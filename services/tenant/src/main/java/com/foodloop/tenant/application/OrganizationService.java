package com.foodloop.tenant.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.tenant.domain.OrgMember;
import com.foodloop.tenant.domain.OrgMemberRepository;
import com.foodloop.tenant.domain.OrgMemberRole;
import com.foodloop.tenant.domain.Organization;
import com.foodloop.tenant.domain.OrganizationRepository;
import com.foodloop.tenant.domain.OrganizationType;
import com.foodloop.tenant.infrastructure.events.OrganizationEventPublisher;
import java.util.List;
import java.util.UUID;
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
    public Organization createOrganization(UUID tenantId, UUID callerUserId, String name, OrganizationType type) {
        Organization organization = organizationRepository.save(new Organization(tenantId, name, type));
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
    public Organization renameOrganization(UUID organizationId, UUID callerUserId, String newName) {
        requireAdmin(organizationId, callerUserId);
        Organization organization = getOrganization(organizationId);
        organization.rename(newName);
        return organizationRepository.save(organization);
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
