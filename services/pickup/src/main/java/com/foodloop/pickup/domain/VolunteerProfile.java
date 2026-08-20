package com.foodloop.pickup.domain;

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
 * A person who has opted into the VOLUNTEER realm role and registered to
 * perform pickup/delivery tasks (spec Phase 10). Existence of this row —
 * not a fresh JWT role check — is what {@link com.foodloop.pickup.application.PickupService}'s
 * volunteer actions gate on: see PickupTaskController's Javadoc for why.
 */
@Entity
@Table(name = "volunteer_profile", schema = "pickup")
public class VolunteerProfile {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "capacity_servings")
    private Integer capacityServings;

    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "current_location", columnDefinition = "geography(Point,4326)")
    private Point currentLocation;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected VolunteerProfile() {
        // JPA
    }

    public VolunteerProfile(UUID tenantId, UUID userId, VehicleType vehicleType, Integer capacityServings) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.vehicleType = vehicleType;
        this.capacityServings = capacityServings;
    }

    public void updateAvailability(boolean available) {
        this.available = available;
    }

    public void updateLocation(Point location) {
        this.currentLocation = location;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public Integer getCapacityServings() {
        return capacityServings;
    }

    public boolean isAvailable() {
        return available;
    }

    public Point getCurrentLocation() {
        return currentLocation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
