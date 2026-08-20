package com.foodloop.ai.guardrail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Heuristic scan of user-supplied text for instruction-override patterns,
 * applied before that text reaches system-prompt context (spec §28). This is
 * a defense-in-depth layer, not a guarantee — it complements, and never
 * replaces, the structural controls that actually keep an agent safe: the
 * per-agent tool allowlist ({@link com.foodloop.ai.tool.AgentPermissionRegistry})
 * and server-side re-validation inside every tool's {@code execute}.
 */
@Component
public class PromptInjectionGuard {

    private static final Map<String, Pattern> SIGNALS = Map.of(
            "ignore-previous-instructions",
            Pattern.compile("ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions", Pattern.CASE_INSENSITIVE),
            "disregard-instructions",
            Pattern.compile("disregard\\s+(all\\s+)?(previous|prior|above)", Pattern.CASE_INSENSITIVE),
            "role-override",
            Pattern.compile("you\\s+are\\s+now\\s+(a|an)\\b", Pattern.CASE_INSENSITIVE),
            "reveal-system-prompt",
            Pattern.compile("(reveal|show|print)\\s+(your|the)\\s+(system\\s+)?prompt", Pattern.CASE_INSENSITIVE),
            "new-instructions-marker",
            Pattern.compile("new\\s+instructions\\s*:", Pattern.CASE_INSENSITIVE),
            "developer-mode",
            Pattern.compile("developer\\s+mode", Pattern.CASE_INSENSITIVE));

    public PromptInjectionScanResult scan(String userSuppliedText) {
        if (userSuppliedText == null || userSuppliedText.isBlank()) {
            return PromptInjectionScanResult.clean();
        }
        List<String> matched = new ArrayList<>();
        for (Map.Entry<String, Pattern> signal : SIGNALS.entrySet()) {
            if (signal.getValue().matcher(userSuppliedText).find()) {
                matched.add(signal.getKey());
            }
        }
        return matched.isEmpty() ? PromptInjectionScanResult.clean() : new PromptInjectionScanResult(true, List.copyOf(matched));
    }

    /** Delimiter-fences untrusted text so a model is steered to treat it as data, never as instructions. */
    public String fence(String userSuppliedText) {
        return "<user_supplied_untrusted_content>\n" + userSuppliedText + "\n</user_supplied_untrusted_content>";
    }
}
