package com.foodloop.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.locationtech.jts.geom.Point;

/**
 * A business entity within a tenant — a restaurant, NGO, corporate donor,
 * etc. (docs/architecture/01-bounded-contexts.md). Tenant-isolated via RLS
 * (V1__create_tenant_and_organization.sql); the isolation itself is
 * enforced by Postgres, not by this class.
 */
@Entity
@Table(name = "organization", schema = "tenant")
public class Organization {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;

    /** Set only for receiver-capable orgs that opt into location-based matching (Phase 7) — donor orgs leave this null. */
    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Organization() {
        // JPA
    }

    public Organization(UUID tenantId, String name, OrganizationType type) {
        this.tenantId = tenantId;
        this.name = name;
        this.type = type;
    }

    public void rename(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public void updateLocation(Point location) {
        this.location = location;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public OrganizationType getType() {
        return type;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public Point getLocation() {
        return location;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
