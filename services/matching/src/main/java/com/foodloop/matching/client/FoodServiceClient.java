package com.foodloop.matching.client;

import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * MatchingService's re-validation path (§17: "createMatchProposal itself
 * re-validates eligibility server-side before insert") calls this to fetch
 * the authoritative listing state, never trusting whatever the agent's tool
 * call input claims.
 */
@Component
@EnableConfigurationProperties(FoodServiceProperties.class)
public class FoodServiceClient {

    private final RestClient restClient;
    private final ServiceAccountTokenProvider tokenProvider;

    public FoodServiceClient(FoodServiceProperties properties, ServiceAccountTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.tokenProvider = tokenProvider;
    }

    public FoodListingDto getFoodListing(UUID tenantId, UUID listingId) {
        return restClient.get()
                .uri("/api/v1/food-listings/{id}", listingId)
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(FoodListingDto.class);
    }

    private void authorize(HttpHeaders headers, UUID tenantId) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set("X-Tenant-Id", tenantId.toString());
    }
}
