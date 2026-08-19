package com.foodloop.tenant.api;

import com.foodloop.tenant.domain.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrganizationRequest(@NotBlank String name, @NotNull OrganizationType type) {
}
