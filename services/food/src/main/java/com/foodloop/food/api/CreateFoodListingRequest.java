package com.foodloop.food.api;

import com.foodloop.food.domain.FoodCategory;
import com.foodloop.food.domain.QuantityUnit;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateFoodListingRequest(
        @NotNull UUID donorOrgId,
        @NotBlank String title,
        String description,
        @NotNull FoodCategory foodCategory,
        List<String> dietaryTypes,
        List<String> allergens,
        @NotNull @Positive BigDecimal quantityValue,
        @NotNull QuantityUnit quantityUnit,
        Integer estimatedServings,
        Instant preparationTime,
        @NotNull @Future Instant expiryTime,
        @NotNull Instant pickupStartTime,
        @NotNull Instant pickupEndTime,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude) {
}
