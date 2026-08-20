package com.foodloop.ai.provider;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ADR-008's fallback chain: primary provider, then whichever secondary
 * providers are registered (Azure AI Foundry would slot in here as another
 * {@link ChatModelProvider} bean, same interface — not built yet, no
 * agent needs it until one specifically requires Foundry's model catalog),
 * then {@link MockChatModelProvider} as the guaranteed last resort. Spring
 * injects providers ordered by {@code @Order}; this class doesn't hardcode
 * which providers exist, only the "try each until one is available" policy.
 */
@Component
public class ChatModelProviderChain {

    private static final Logger log = LoggerFactory.getLogger(ChatModelProviderChain.class);

    private final List<ChatModelProvider> providersInOrder;

    public ChatModelProviderChain(List<ChatModelProvider> providersInOrder) {
        this.providersInOrder = providersInOrder;
    }

    public ChatCompletionResult complete(String systemPrompt, String userPrompt) {
        for (ChatModelProvider provider : providersInOrder) {
            if (!provider.isAvailable()) {
                continue;
            }
            try {
                return provider.complete(systemPrompt, userPrompt);
            } catch (RuntimeException e) {
                log.warn("Provider {} failed, falling back to next in chain", provider.name(), e);
            }
        }
        throw new IllegalStateException("No chat model provider was available, including the mock fallback — this should never happen.");
    }
}
