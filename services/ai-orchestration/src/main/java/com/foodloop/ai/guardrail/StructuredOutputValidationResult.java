package com.foodloop.ai.guardrail;

import com.fasterxml.jackson.databind.JsonNode;

public record StructuredOutputValidationResult(boolean valid, JsonNode parsed, String errorMessage) {

    public static StructuredOutputValidationResult valid(JsonNode parsed) {
        return new StructuredOutputValidationResult(true, parsed, null);
    }

    public static StructuredOutputValidationResult invalid(String errorMessage) {
        return new StructuredOutputValidationResult(false, null, errorMessage);
    }
}
