package com.foodloop.food.api;

import com.foodloop.food.domain.FoodListing;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FoodListingResponse(
        UUID id,
        UUID tenantId,
        UUID donorOrgId,
        String title,
        String description,
        String foodCategory,
        List<String> dietaryTypes,
        List<String> allergens,
        BigDecimal quantityValue,
        String quantityUnit,
        Integer estimatedServings,
        Instant expiryTime,
        Instant pickupStartTime,
        Instant pickupEndTime,
        double latitude,
        double longitude,
        String status,
        String verificationStatus,
        Instant createdAt) {

    public static FoodListingResponse from(FoodListing listing) {
        return new FoodListingResponse(
                listing.getId(),
                listing.getTenantId(),
                listing.getDonorOrgId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getFoodCategory().name(),
                listing.getDietaryTypes(),
                listing.getAllergens(),
                listing.getQuantityValue(),
                listing.getQuantityUnit().name(),
                listing.getEstimatedServings(),
                listing.getExpiryTime(),
                listing.getPickupStartTime(),
                listing.getPickupEndTime(),
                listing.getLocation().getY(),
                listing.getLocation().getX(),
                listing.getStatus().name(),
                listing.getVerificationStatus().name(),
                listing.getCreatedAt());
    }

    /** Public search results carry the jittered point, never the exact one (§33). */
    public static FoodListingResponse fromPublic(FoodListing listing) {
        return new FoodListingResponse(
                listing.getId(),
                listing.getTenantId(),
                listing.getDonorOrgId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getFoodCategory().name(),
                listing.getDietaryTypes(),
                listing.getAllergens(),
                listing.getQuantityValue(),
                listing.getQuantityUnit().name(),
                listing.getEstimatedServings(),
                listing.getExpiryTime(),
                listing.getPickupStartTime(),
                listing.getPickupEndTime(),
                listing.getApproxLocation().getY(),
                listing.getApproxLocation().getX(),
                listing.getStatus().name(),
                listing.getVerificationStatus().name(),
                listing.getCreatedAt());
    }
}
