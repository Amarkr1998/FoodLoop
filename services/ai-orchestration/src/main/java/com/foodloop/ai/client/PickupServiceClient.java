package com.foodloop.ai.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** The Pickup Agent's path to the Pickup bounded context (spec §20) — same authenticated-boundary pattern as FoodServiceClient. */
@Component
@EnableConfigurationProperties(PickupServiceProperties.class)
public class PickupServiceClient {

    private final RestClient restClient;
    private final ServiceAccountTokenProvider tokenProvider;

    public PickupServiceClient(PickupServiceProperties properties, ServiceAccountTokenProvider tokenProvider) {
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.tokenProvider = tokenProvider;
    }

    public PickupTaskDto getTask(UUID tenantId, UUID taskId) {
        return restClient.get()
                .uri("/api/v1/pickups/{id}", taskId)
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(PickupTaskDto.class);
    }

    public List<VolunteerProfileDto> findNearbyVolunteers(UUID tenantId, UUID taskId, double radiusKm) {
        List<VolunteerProfileDto> volunteers = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/pickups/{id}/nearby-volunteers")
                        .queryParam("radiusKm", radiusKm)
                        .build(taskId))
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<VolunteerProfileDto>>() {
                });
        return volunteers != null ? volunteers : List.of();
    }

    /** The Pickup Agent's scheduled sweep (spec §20) — assigned tasks past their scheduled window. */
    public List<PickupTaskDto> findDelayed(UUID tenantId, Instant asOf) {
        List<PickupTaskDto> tasks = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/pickups/delayed").queryParam("asOf", asOf).build())
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<PickupTaskDto>>() {
                });
        return tasks != null ? tasks : List.of();
    }

    public PickupTaskDto systemUnassign(UUID tenantId, UUID taskId) {
        return restClient.post()
                .uri("/api/v1/pickups/{id}/system-unassign", taskId)
                .headers(headers -> authorize(headers, tenantId))
                .retrieve()
                .body(PickupTaskDto.class);
    }

    private void authorize(HttpHeaders headers, UUID tenantId) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set("X-Tenant-Id", tenantId.toString());
    }
}
