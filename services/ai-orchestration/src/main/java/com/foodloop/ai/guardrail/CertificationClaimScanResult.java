package com.foodloop.ai.guardrail;

import java.util.List;

public record CertificationClaimScanResult(boolean violatesPolicy, List<String> matchedSignals) {

    public static CertificationClaimScanResult clean() {
        return new CertificationClaimScanResult(false, List.of());
    }
}
