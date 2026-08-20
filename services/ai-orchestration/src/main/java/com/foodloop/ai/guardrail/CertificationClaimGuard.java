package com.foodloop.ai.guardrail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The Safety Agent's hard constraint (spec §22: "It never asserts legal/medical
 * certification — that's a hard prompt/system constraint plus an output
 * validator that rejects any generated text matching a certification-claim
 * pattern"). Scans the agent's own generated text — never user-supplied
 * text, that's {@link PromptInjectionGuard}'s job — so a violation here
 * means the model itself produced a disallowed claim and its output must
 * never be persisted, not that anything upstream did something wrong.
 */
@Component
public class CertificationClaimGuard {

    private static final Map<String, Pattern> SIGNALS = Map.of(
            "fda-claim", Pattern.compile("FDA[- ]?(approved|certified|cleared)", Pattern.CASE_INSENSITIVE),
            "certified-safe-claim", Pattern.compile("certified\\s+(safe|organic|healthy)", Pattern.CASE_INSENSITIVE),
            "medically-safe-claim", Pattern.compile("medically\\s+(safe|approved|verified)", Pattern.CASE_INSENSITIVE),
            "guaranteed-safe-claim", Pattern.compile("guarantee(d)?\\s+(safe|allergen[- ]?free)", Pattern.CASE_INSENSITIVE),
            "health-code-claim", Pattern.compile("meets?\\s+(all\\s+)?health\\s+code", Pattern.CASE_INSENSITIVE),
            "legally-safe-claim", Pattern.compile("legally\\s+(safe|compliant|certified)", Pattern.CASE_INSENSITIVE));

    public CertificationClaimScanResult scan(String generatedText) {
        if (generatedText == null || generatedText.isBlank()) {
            return CertificationClaimScanResult.clean();
        }
        List<String> matched = new ArrayList<>();
        for (Map.Entry<String, Pattern> signal : SIGNALS.entrySet()) {
            if (signal.getValue().matcher(generatedText).find()) {
                matched.add(signal.getKey());
            }
        }
        return matched.isEmpty()
                ? CertificationClaimScanResult.clean()
                : new CertificationClaimScanResult(true, List.copyOf(matched));
    }
}
