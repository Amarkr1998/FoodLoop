package com.foodloop.food.api;

import com.foodloop.food.domain.FoodAiMetadata;
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
        AiMetadataView aiMetadata,
        boolean requiresSafetyReview,
        String safetyReviewReason,
        Instant createdAt) {

    /** Null until the Food Intelligence Agent has analyzed this listing (spec §16) — advisory, never authoritative. */
    public record AiMetadataView(
            String category,
            List<String> dietaryTypes,
            List<String> allergens,
            Integer estimatedServings,
            String urgency,
            List<String> missingInformation,
            String suggestedDescription,
            Double confidence,
            Instant analyzedAt) {

        static AiMetadataView from(FoodAiMetadata metadata) {
            if (metadata == null) {
                return null;
            }
            return new AiMetadataView(
                    metadata.category(), metadata.dietaryTypes(), metadata.allergens(), metadata.estimatedServings(),
                    metadata.urgency(), metadata.missingInformation(), metadata.suggestedDescription(),
                    metadata.confidence(), metadata.analyzedAt());
        }
    }

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
                AiMetadataView.from(listing.getAiMetadata()),
                listing.isRequiresSafetyReview(),
                listing.getSafetyReviewReason(),
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
                AiMetadataView.from(listing.getAiMetadata()),
                listing.isRequiresSafetyReview(),
                listing.getSafetyReviewReason(),
                listing.getCreatedAt());
    }
}
