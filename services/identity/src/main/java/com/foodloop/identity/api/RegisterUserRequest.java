package com.foodloop.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * {@code tenantId} is accepted directly for now rather than resolved from
 * an org invite code or region lookup — the Organization &amp; Tenant
 * bounded context (docs/architecture/01-bounded-contexts.md) that would own
 * tenant resolution/validation is the next vertical slice after this one.
 */
public record RegisterUserRequest(
        @NotNull UUID tenantId,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
        @NotBlank String displayName,
        String locale) {
}
