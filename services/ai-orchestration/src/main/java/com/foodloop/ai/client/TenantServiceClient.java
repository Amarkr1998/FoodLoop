package com.foodloop.ai.client;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The Rescue Agent's scheduled sweep (Phase 8) needs to know every active
 * tenant to check, which is inherently a platform-level query, not a
 * tenant-scoped one — no {@code X-Tenant-Id} header on this call, unlike
 * every other client in this package, because there is no single tenant to
 * scope it to (see Tenant's own TenantController Javadoc).
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

    public List<TenantDto> listActiveTenants() {
        List<TenantDto> tenants = restClient.get()
                .uri("/api/v1/tenants")
                .headers(headers -> headers.setBearerAuth(tokenProvider.getAccessToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<List<TenantDto>>() {
                });
        return tenants != null ? tenants : List.of();
    }
}
