package com.foodloop.ngo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * One row per NGO org (spec §19's "NGO requirements") — what the NGO
 * Coordination Agent's {@code getNGORequirements} tool reads before
 * searching for food to propose. Separate from Organization &amp; Tenant's
 * generic organization record (ADR-003): this is NGO-specific operating
 * data, not identity/verification data.
 */
@Entity
@Table(name = "ngo_requirement", schema = "ngo")
public class NgoRequirement {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "ngo_org_id", nullable = false, updatable = false)
    private UUID ngoOrgId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preferred_categories")
    private String[] preferredCategories;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dietary_restrictions")
    private String[] dietaryRestrictions;

    @Column(name = "capacity_per_week")
    private Integer capacityPerWeek;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NgoRequirement() {
        // JPA
    }

    public NgoRequirement(UUID tenantId, UUID ngoOrgId) {
        this.tenantId = tenantId;
        this.ngoOrgId = ngoOrgId;
    }

    public void update(String[] preferredCategories, String[] dietaryRestrictions, Integer capacityPerWeek) {
        this.preferredCategories = preferredCategories;
        this.dietaryRestrictions = dietaryRestrictions;
        this.capacityPerWeek = capacityPerWeek;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getNgoOrgId() {
        return ngoOrgId;
    }

    public String[] getPreferredCategories() {
        return preferredCategories;
    }

    public String[] getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public Integer getCapacityPerWeek() {
        return capacityPerWeek;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
