package com.foodloop.ai.guardrail;

import java.util.List;

public record PromptInjectionScanResult(boolean suspicious, List<String> matchedSignals) {

    public static PromptInjectionScanResult clean() {
        return new PromptInjectionScanResult(false, List.of());
    }
}
