package com.foodloop.ai.api;

import com.foodloop.ai.agent.rescue.RescueThreshold;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RescueCheckRequest(@NotNull UUID foodListingId, @NotNull RescueThreshold threshold) {
}
