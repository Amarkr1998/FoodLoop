package com.foodloop.food.api;

import com.foodloop.food.domain.Claim;
import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(
        UUID id, UUID foodListingId, UUID receiverUserId, String status, Instant claimedAt, Instant expiresAt) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(
                claim.getId(), claim.getFoodListingId(), claim.getReceiverUserId(),
                claim.getStatus().name(), claim.getClaimedAt(), claim.getExpiresAt());
    }
}
