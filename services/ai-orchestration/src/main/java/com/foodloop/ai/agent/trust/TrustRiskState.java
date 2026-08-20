package com.foodloop.ai.agent.trust;

import com.foodloop.ai.client.ReportDto;
import com.foodloop.ai.client.RiskCaseDto;
import com.foodloop.ai.client.UserBehaviorSignalDto;
import java.util.List;
import java.util.UUID;

record TrustRiskState(
        UUID targetUserId,
        UserBehaviorSignalDto signals,
        List<ReportDto> reports,
        String providerName,
        String modelName,
        String rawModelOutput,
        TrustRiskLlmOutput llmOutput,
        String escalationReason,
        int retryCount,
        RiskCaseDto riskCase) {

    static TrustRiskState initial(UUID targetUserId) {
        return new TrustRiskState(targetUserId, null, null, null, null, null, null, null, 0, null);
    }

    TrustRiskState withSignals(UserBehaviorSignalDto signals) {
        return new TrustRiskState(targetUserId, signals, reports, providerName, modelName, rawModelOutput,
                llmOutput, escalationReason, retryCount, riskCase);
    }

    TrustRiskState withReports(List<ReportDto> reports) {
        return new TrustRiskState(targetUserId, signals, reports, providerName, modelName, rawModelOutput,
                llmOutput, escalationReason, retryCount, riskCase);
    }

    TrustRiskState withModelOutput(String providerName, String modelName, String rawModelOutput) {
        return new TrustRiskState(targetUserId, signals, reports, providerName, modelName, rawModelOutput,
                llmOutput, escalationReason, retryCount, riskCase);
    }

    TrustRiskState withLlmOutput(TrustRiskLlmOutput llmOutput) {
        return new TrustRiskState(targetUserId, signals, reports, providerName, modelName, rawModelOutput,
                llmOutput, null, retryCount, riskCase);
    }

    TrustRiskState withValidationFailure(String reason) {
        return new TrustRiskState(targetUserId, signals, reports, providerName, modelName, rawModelOutput,
                null, reason, retryCount + 1, riskCase);
    }

    TrustRiskState withRiskCase(RiskCaseDto riskCase) {
        return new TrustRiskState(targetUserId, signals, reports, providerName, modelName, rawModelOutput,
                llmOutput, escalationReason, retryCount, riskCase);
    }
}
