package com.foodloop.tenant.api;

import com.foodloop.tenant.domain.OrganizationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * {@code latitude}/{@code longitude} are optional — only receiver-capable
 * orgs that want to appear in the Matching Agent's nearby search need to set
 * a location at all (Phase 7); a donor org typically leaves both null.
 */
public record CreateOrganizationRequest(
        @NotBlank String name,
        @NotNull OrganizationType type,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude) {
}
