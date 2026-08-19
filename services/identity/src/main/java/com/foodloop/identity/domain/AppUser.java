package com.foodloop.identity.domain;

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

/**
 * A user's platform profile. Credentials, MFA, and login live in Keycloak
 * (ADR-004) — this row is the tenant-scoped profile that the rest of the
 * platform (and RLS-protected joins from other bounded contexts) refers to.
 */
@Entity
@Table(name = "app_user", schema = "identity")
public class AppUser {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "keycloak_id", nullable = false, updatable = false)
    private UUID keycloakId;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String locale = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected AppUser() {
        // JPA
    }

    public AppUser(UUID tenantId, UUID keycloakId, String email, String displayName, String locale) {
        this.tenantId = tenantId;
        this.keycloakId = keycloakId;
        this.email = email;
        this.displayName = displayName;
        this.locale = (locale != null && !locale.isBlank()) ? locale : "en";
    }

    public void updateProfile(String displayName, String phone, String locale) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (locale != null && !locale.isBlank()) {
            this.locale = locale;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getKeycloakId() {
        return keycloakId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLocale() {
        return locale;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
