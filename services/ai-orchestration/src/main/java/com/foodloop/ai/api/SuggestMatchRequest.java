package com.foodloop.ai.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SuggestMatchRequest(@NotNull UUID foodListingId) {
}
