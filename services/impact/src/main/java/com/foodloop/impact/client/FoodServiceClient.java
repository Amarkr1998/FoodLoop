package com.foodloop.impact.client;

import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Enriches a bare pickup.completed.v1 event (listing id only) with the
 * quantity/category/donor-org data the Impact fact table needs — real HTTP
 * call through the authenticated boundary, same pattern as every other
 * cross-service client in this platform (docs/architecture/05 §1, even
 * though this consumer isn't an AI agent).
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
