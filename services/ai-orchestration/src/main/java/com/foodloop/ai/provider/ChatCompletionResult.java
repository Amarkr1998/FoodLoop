package com.foodloop.ai.provider;

public record ChatCompletionResult(String providerName, String modelName, String content, int totalTokens) {
}
