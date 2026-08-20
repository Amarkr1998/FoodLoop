package com.foodloop.ai.tool.food;

import java.util.UUID;

public record CreateSafetyCaseCommand(UUID foodListingId, String reason) {
}
