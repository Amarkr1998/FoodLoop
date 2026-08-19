package com.foodloop.tenant.api;

import com.foodloop.tenant.domain.OrgMemberRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(@NotNull UUID userId, @NotNull OrgMemberRole role) {
}
