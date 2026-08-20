package com.foodloop.food.api;

import jakarta.validation.constraints.NotBlank;

public record FlagSafetyReviewRequest(@NotBlank String reason) {
}
