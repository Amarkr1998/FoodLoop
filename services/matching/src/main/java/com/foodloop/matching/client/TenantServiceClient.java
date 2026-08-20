package com.foodloop.matching.client;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Both the deterministic candidate search (nearby receiver orgs) and the
 * per-proposal re-validation (does this specific org still qualify?) go
 * through here — same real HTTP call either way, never a shortcut for the
 * "just double-checking" path.
 */
@Component
@EnableConfigurationProperties(TenantServiceProperties.class)
public class TenantServiceClient {

    private final RestClient restClient;
    private final ServiceAccountTokenProvider tokenProvider;

    public TenantServiceClient(TenantServiceProperties properties, ServiceAccountTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.tokenProvider = tokenProvider;
    }

    public OrganizationDto getOrganization(UUID tenantId, UUID organizationId) {
        return restClient.get()
                .uri("/api/v1/organizations/{id}", organizationId)
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(OrganizationDto.class);
    }

    public List<OrganizationDto> searchNearbyReceivers(
            UUID tenantId, double lat, double lng, double radiusKm, String type) {
        PageDto<OrganizationDto> page = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/organizations")
                        .queryParam("lat", lat)
                        .queryParam("lng", lng)
                        .queryParam("radiusKm", radiusKm)
                        .queryParamIfPresent("type", java.util.Optional.ofNullable(type))
                        .queryParam("size", 50)
                        .build())
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(new ParameterizedTypeReference<PageDto<OrganizationDto>>() {
                });
        return page != null ? page.content() : List.of();
    }

    private void authorize(HttpHeaders headers, UUID tenantId) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set("X-Tenant-Id", tenantId.toString());
    }
}
