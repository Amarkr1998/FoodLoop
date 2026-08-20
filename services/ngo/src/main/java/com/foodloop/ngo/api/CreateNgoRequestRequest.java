package com.foodloop.ngo.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateNgoRequestRequest(
        @NotNull UUID ngoOrgId, @NotNull String foodCategory, @NotNull @Positive BigDecimal quantityNeeded,
        @NotNull String quantityUnit, @NotNull Instant neededBefore, String notes) {
}
