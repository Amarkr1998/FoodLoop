package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NgoRequestDto(
        UUID id, UUID ngoOrgId, String foodCategory, BigDecimal quantityNeeded, String quantityUnit,
        Instant neededBefore, String status) {
}
