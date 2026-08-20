package com.foodloop.ai.tool.matching;

import java.util.UUID;

public record CreateMatchProposalCommand(UUID foodListingId, UUID receiverOrgId, String aiRationale) {
}
