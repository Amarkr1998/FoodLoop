package com.foodloop.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A real, working provider — not a stub — but explicitly a development-only
 * one: deterministic, pattern-based, clearly marked in every response and
 * every log line so its use is never mistaken for a genuine model
 * completion (spec §55: no disguising deterministic logic as AI). Always
 * available, so it's the guaranteed last resort in
 * {@link ChatModelProviderChain} when no real provider is configured or
 * reachable (ADR-008) — critical paths must survive an AI outage, and this
 * is what lets local dev run at all without Azure credentials
 * (.env.example's AI_PROVIDER_MODE=mock).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class MockChatModelProvider implements ChatModelProvider {

    private static final Logger log = LoggerFactory.getLogger(MockChatModelProvider.class);

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ChatCompletionResult complete(String systemPrompt, String userPrompt) {
        log.warn("MOCK chat model provider invoked — no real model call was made. "
                + "This must never happen with a production provider configured.");
        String content = "[MOCK RESPONSE] Deterministic development stand-in for prompt of "
                + userPrompt.length() + " characters. No real model was called.";
        return new ChatCompletionResult(name(), "mock-model", content, 0);
    }
}
