package com.foodloop.food;

import com.foodloop.food.api.CreateFoodListingRequest;
import com.foodloop.food.domain.FoodCategory;
import com.foodloop.food.domain.QuantityUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

final class FoodTestSupport {

    private FoodTestSupport() {
    }

    static CreateFoodListingRequest sampleRequest(UUID donorOrgId) {
        Instant now = Instant.now();
        return new CreateFoodListingRequest(
                donorOrgId,
                "Test Meal",
                "A test listing",
                FoodCategory.COOKED_MEAL,
                List.of("VEGETARIAN"),
                List.of(),
                BigDecimal.valueOf(20),
                QuantityUnit.SERVINGS,
                20,
                now,
                now.plus(6, ChronoUnit.HOURS),
                now.plus(1, ChronoUnit.HOURS),
                now.plus(3, ChronoUnit.HOURS),
                BigDecimal.valueOf(12.9716),
                BigDecimal.valueOf(77.5946));
    }
}
