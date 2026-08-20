package com.foodloop.ai.client;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The Food Intelligence Agent's only path to the Food bounded context — a
 * real HTTP call through the same authenticated boundary a normal client
 * would use, never a shared database connection
 * (docs/architecture/05-ai-agent-architecture.md §1). Every call carries
 * this service's own service-account bearer token (never the inbound
 * caller's) plus an {@code X-Tenant-Id} header, which only this trusted
 * client identity is permitted to use in place of a JWT tenant_id claim
 * (see {@link com.foodloop.commons.tenant.TenantFilter}'s Javadoc).
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

    public FoodListingDto updateAiMetadata(UUID tenantId, UUID listingId, UpdateAiMetadataPayload payload) {
        return restClient.put()
                .uri("/api/v1/food-listings/{id}/ai-metadata", listingId)
                .headers(headers -> authorize(headers, tenantId))
                .body(payload)
                .retrieve()
                .body(FoodListingDto.class);
    }

    /** The Safety Agent's write path (spec §22) — see FoodListing#flagForSafetyReview: raises the hold, never clears it. */
    public FoodListingDto flagForSafetyReview(UUID tenantId, UUID listingId, String reason) {
        return restClient.put()
                .uri("/api/v1/food-listings/{id}/safety-flag", listingId)
                .headers(headers -> authorize(headers, tenantId))
                .body(new FlagSafetyReviewPayload(reason))
                .retrieve()
                .body(FoodListingDto.class);
    }

    /** The Food Rescue Agent's expiry sweep (spec §18) calls this once per tenant per configured threshold. */
    public List<FoodListingDto> getExpiringListings(UUID tenantId, int withinMinutes) {
        List<FoodListingDto> listings = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/food-listings/expiring")
                        .queryParam("withinMinutes", withinMinutes)
                        .build())
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<FoodListingDto>>() {
                });
        return listings != null ? listings : List.of();
    }

    private void authorize(HttpHeaders headers, UUID tenantId) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set("X-Tenant-Id", tenantId.toString());
    }
}
