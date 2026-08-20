package com.foodloop.ai.provider;

/**
 * The seam that makes the platform not tightly coupled to one model vendor
 * (spec §29, ADR-005/ADR-008). Every agent talks to this interface, never
 * to a vendor SDK directly.
 */
public interface ChatModelProvider {

    String name();

    /** Cheap, synchronous check — no network call — used to skip a known-unconfigured provider in the chain. */
    boolean isAvailable();

    ChatCompletionResult complete(String systemPrompt, String userPrompt);
}
