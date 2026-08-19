package com.foodloop.commons.tenant;

import java.util.UUID;

/**
 * Holds the current request's tenant for the lifetime of the request thread.
 * Populated by {@link TenantFilter} from the authenticated JWT's {@code tenant_id}
 * claim and read by {@link TenantAwareDataSource} on every connection checkout —
 * the actual enforcement point is the database's row-level-security policy
 * (ADR-009), not this holder; this just carries the value to where it's needed.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
