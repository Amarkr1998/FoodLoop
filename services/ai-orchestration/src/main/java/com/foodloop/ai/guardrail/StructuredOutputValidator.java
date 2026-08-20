package com.foodloop.ai.guardrail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates that an agent's terminal output is well-formed JSON with every
 * required field present (spec §28). Unparseable or schema-invalid output
 * must never be written to a domain table as a raw string — callers are
 * expected to retry once on an invalid result, then route to escalation
 * (docs/architecture/05-ai-agent-architecture.md §8), never to fall back to
 * persisting whatever text the model returned.
 */
@Component
public class StructuredOutputValidator {

    private final ObjectMapper objectMapper;

    public StructuredOutputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public StructuredOutputValidationResult validate(String rawOutput, Set<String> requiredFields) {
        JsonNode node;
        try {
            node = objectMapper.readTree(rawOutput);
        } catch (JsonProcessingException ex) {
            return StructuredOutputValidationResult.invalid("Output is not valid JSON: " + ex.getOriginalMessage());
        }
        if (node == null || !node.isObject()) {
            return StructuredOutputValidationResult.invalid("Output JSON must be an object.");
        }
        List<String> missing = requiredFields.stream()
                .filter(field -> !node.hasNonNull(field))
                .toList();
        if (!missing.isEmpty()) {
            return StructuredOutputValidationResult.invalid("Missing required field(s): " + String.join(", ", missing));
        }
        return StructuredOutputValidationResult.valid(node);
    }
}
