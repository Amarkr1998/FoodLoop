package com.foodloop.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The primary provider (ADR-005). {@link ObjectProvider} rather than a
 * direct {@link ChatModel} dependency because the Azure OpenAI starter's
 * auto-configuration may or may not produce a bean depending on how it's
 * configured across Spring AI versions — this provider must degrade to
 * "unavailable" either way, never fail to start the service, when no real
 * Azure endpoint/key is configured (the default in local dev, see
 * .env.example's AI_PROVIDER_MODE).
 *
 * <p>{@code isAvailable()} deliberately reads the raw {@code AZURE_OPENAI_*}
 * env vars directly (blank when unset) rather than the
 * {@code spring.ai.azure.openai.*} properties: application.yml feeds those
 * from the same env vars but with harmless placeholder defaults, because
 * Spring AI's auto-configuration builds its {@code OpenAIClientBuilder}
 * bean eagerly at context-refresh time and throws if the endpoint is empty
 * — the placeholder keeps that bean construction inert, while this class's
 * own availability check still correctly reports "not configured".
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AzureOpenAiChatModelProvider implements ChatModelProvider {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiChatModelProvider.class);

    private final ObjectProvider<ChatModel> chatModel;
    private final String apiKey;
    private final String endpoint;

    public AzureOpenAiChatModelProvider(
            ObjectProvider<ChatModel> chatModel,
            @Value("${AZURE_OPENAI_API_KEY:}") String apiKey,
            @Value("${AZURE_OPENAI_ENDPOINT:}") String endpoint) {
        this.chatModel = chatModel;
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public String name() {
        return "azure-openai";
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank() && !endpoint.isBlank() && chatModel.getIfAvailable() != null;
    }

    @Override
    public ChatCompletionResult complete(String systemPrompt, String userPrompt) {
        ChatModel model = chatModel.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException("Azure OpenAI ChatModel bean is not available.");
        }
        Prompt prompt = new Prompt(java.util.List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)));
        ChatResponse response = model.call(prompt);
        String content = response.getResult().getOutput().getText();
        int tokens = response.getMetadata().getUsage() != null
                ? (int) response.getMetadata().getUsage().getTotalTokens()
                : 0;
        log.debug("azure-openai completion: {} tokens", tokens);
        return new ChatCompletionResult(name(), response.getMetadata().getModel(), content, tokens);
    }
}
