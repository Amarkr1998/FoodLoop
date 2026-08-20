package com.foodloop.ai.client;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The Matching Agent's path to the Matching bounded context (Phase 7) — same
 * authenticated-boundary pattern as {@link FoodServiceClient}. Candidate
 * search is read-only and re-derives nothing on the agent's side; proposal
 * creation is re-validated server-side by Matching itself
 * (MatchingService#createProposal), never trusted from this call alone.
 */
@Component
@EnableConfigurationProperties(MatchingServiceProperties.class)
public class MatchingServiceClient {

    private final RestClient restClient;
    private final ServiceAccountTokenProvider tokenProvider;

    public MatchingServiceClient(MatchingServiceProperties properties, ServiceAccountTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.tokenProvider = tokenProvider;
    }

    public List<MatchCandidateDto> getCandidates(UUID tenantId, UUID foodListingId, double radiusKm) {
        List<MatchCandidateDto> candidates = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/matches/candidates")
                        .queryParam("foodListingId", foodListingId)
                        .queryParam("radiusKm", radiusKm)
                        .build())
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<MatchCandidateDto>>() {
                });
        return candidates != null ? candidates : List.of();
    }

    public MatchProposalDto createProposal(UUID tenantId, UUID foodListingId, UUID receiverOrgId, String aiRationale) {
        return createProposal(tenantId, foodListingId, receiverOrgId, aiRationale, null);
    }

    public MatchProposalDto createProposal(
            UUID tenantId, UUID foodListingId, UUID receiverOrgId, String aiRationale, UUID ngoRequestId) {
        return restClient.post()
                .uri("/api/v1/matches")
                .headers(headers -> authorize(headers, tenantId))
                .body(new CreateMatchProposalPayload(foodListingId, receiverOrgId, aiRationale, ngoRequestId))
                .retrieve()
                .body(MatchProposalDto.class);
    }

    private void authorize(HttpHeaders headers, UUID tenantId) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set("X-Tenant-Id", tenantId.toString());
    }
}
