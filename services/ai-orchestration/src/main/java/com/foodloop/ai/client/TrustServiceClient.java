package com.foodloop.ai.client;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** The Trust & Risk Agent's path to the Trust &amp; Safety bounded context (spec §21) — same authenticated-boundary pattern as FoodServiceClient. */
@Component
@EnableConfigurationProperties(TrustServiceProperties.class)
public class TrustServiceClient {

    private final RestClient restClient;
    private final ServiceAccountTokenProvider tokenProvider;

    public TrustServiceClient(TrustServiceProperties properties, ServiceAccountTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.tokenProvider = tokenProvider;
    }

    public UserBehaviorSignalDto getSignals(UUID tenantId, UUID targetUserId) {
        return restClient.get()
                .uri("/api/v1/trust/signals/{targetUserId}", targetUserId)
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(UserBehaviorSignalDto.class);
    }

    public List<ReportDto> getReportHistory(UUID tenantId, UUID targetUserId) {
        List<ReportDto> reports = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/trust/reports").queryParam("targetUserId", targetUserId).build())
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<ReportDto>>() {
                });
        return reports != null ? reports : List.of();
    }

    public RiskCaseDto createRiskCase(UUID tenantId, UUID targetUserId, String riskFactors) {
        return restClient.post()
                .uri("/api/v1/trust/risk-cases")
                .headers(headers -> authorize(headers, tenantId))
                .body(new CreateRiskCasePayload(targetUserId, riskFactors))
                .retrieve()
                .body(RiskCaseDto.class);
    }

    private void authorize(HttpHeaders headers, UUID tenantId) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set("X-Tenant-Id", tenantId.toString());
    }
}
