package com.foodloop.ai.client;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** The NGO Coordination Agent's path to the NGO bounded context (spec §19) — same authenticated-boundary pattern as FoodServiceClient. */
@Component
@EnableConfigurationProperties(NgoServiceProperties.class)
public class NgoServiceClient {

    private final RestClient restClient;
    private final ServiceAccountTokenProvider tokenProvider;

    public NgoServiceClient(NgoServiceProperties properties, ServiceAccountTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.tokenProvider = tokenProvider;
    }

    public NgoRequirementDto getRequirements(UUID tenantId, UUID ngoOrgId) {
        return restClient.get()
                .uri("/api/v1/ngo/requirements/{ngoOrgId}", ngoOrgId)
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(NgoRequirementDto.class);
    }

    public NgoRequestDto getRequest(UUID tenantId, UUID ngoRequestId) {
        return restClient.get()
                .uri("/api/v1/ngo/requests/{id}", ngoRequestId)
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(NgoRequestDto.class);
    }

    /** The scheduled sweep's trigger set (spec §19) — open requests nearing their deadline. */
    public List<NgoRequestDto> listOpenRequestsNearingDeadline(UUID tenantId, java.time.Instant neededBeforeOrAt) {
        List<NgoRequestDto> requests = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/ngo/requests/open")
                        .queryParam("neededBeforeOrAt", neededBeforeOrAt)
                        .build())
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<NgoRequestDto>>() {
                });
        return requests != null ? requests : List.of();
    }

    private void authorize(HttpHeaders headers, UUID tenantId) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set("X-Tenant-Id", tenantId.toString());
    }
}
