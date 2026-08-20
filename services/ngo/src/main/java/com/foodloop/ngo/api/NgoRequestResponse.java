package com.foodloop.ngo.api;

import com.foodloop.ngo.domain.NgoRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NgoRequestResponse(
        UUID id, UUID tenantId, UUID ngoOrgId, String foodCategory, BigDecimal quantityNeeded, String quantityUnit,
        Instant neededBefore, String notes, String status, UUID matchedProposalId, UUID matchedFoodListingId,
        Instant createdAt) {

    public static NgoRequestResponse from(NgoRequest request) {
        return new NgoRequestResponse(
                request.getId(), request.getTenantId(), request.getNgoOrgId(), request.getFoodCategory(),
                request.getQuantityNeeded(), request.getQuantityUnit(), request.getNeededBefore(), request.getNotes(),
                request.getStatus().name(), request.getMatchedProposalId(), request.getMatchedFoodListingId(),
                request.getCreatedAt());
    }
}
