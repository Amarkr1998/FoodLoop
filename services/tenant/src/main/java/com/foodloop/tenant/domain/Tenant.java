package com.foodloop.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The region/country-level isolation boundary itself (ADR-009,
 * docs/architecture/02-database-design.md) — deliberately not RLS-protected
 * since a tenant row isn't scoped BY a tenant. Small, admin-managed
 * reference data (see V2__seed_default_tenant.sql for the single-region MVP
 * seed row); read broadly, written only by platform admins.
 */
@Entity
@Table(name = "tenant", schema = "tenant")
public class Tenant {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "region_id", nullable = false)
    private String regionId;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Tenant() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public TenantStatus getStatus() {
        return status;
    }
}
