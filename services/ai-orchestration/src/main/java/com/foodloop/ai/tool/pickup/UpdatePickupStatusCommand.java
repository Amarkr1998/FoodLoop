package com.foodloop.ai.tool.pickup;

import java.util.UUID;

/** Only one action currently exists behind this tool — system-initiated unassignment — see UpdatePickupStatusTool's Javadoc. */
public record UpdatePickupStatusCommand(UUID pickupTaskId) {
}
