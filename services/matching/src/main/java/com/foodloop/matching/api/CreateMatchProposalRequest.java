package com.foodloop.matching.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateMatchProposalRequest(@NotNull UUID foodListingId, @NotNull UUID receiverOrgId, String aiRationale) {
}
