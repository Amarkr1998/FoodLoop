package com.foodloop.ai.client;

import java.util.UUID;

/** Mirrors Matching's CreateMatchProposalRequest — the wire shape of the POST /api/v1/matches request body. */
public record CreateMatchProposalPayload(UUID foodListingId, UUID receiverOrgId, String aiRationale) {
}
