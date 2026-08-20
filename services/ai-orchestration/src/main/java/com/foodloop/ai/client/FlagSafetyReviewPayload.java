package com.foodloop.ai.client;

/** Mirrors Food's FlagSafetyReviewRequest — the wire shape of the PUT safety-flag request body. */
public record FlagSafetyReviewPayload(String reason) {
}
