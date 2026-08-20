package com.foodloop.matching.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * OAuth2 client-credentials grant (RFC 6749 §4.4) for this service's own
 * Keycloak identity ({@code foodloop-matching}) — every outbound call to
 * Food/Tenant to re-validate a proposed match authenticates with a token
 * from here, never with the inbound caller's own token
 * (docs/architecture/05-ai-agent-architecture.md §1). Same implementation as
 * services/ai-orchestration's ServiceAccountTokenProvider — duplicated
 * rather than shared because backend-commons is deliberately kept free of
 * anything beyond tenant/error/correlation infra (ADR-003), and this is the
 * only other service that needs it so far.
 */
@Component
@EnableConfigurationProperties(ServiceAccountProperties.class)
public class ServiceAccountTokenProvider {

    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 30;

    private final RestClient restClient;
    private final ServiceAccountProperties properties;

    private volatile CachedToken cachedToken;

    public ServiceAccountTokenProvider(ServiceAccountProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public synchronized String getAccessToken() {
        CachedToken current = cachedToken;
        if (current != null && current.expiresAt.isAfter(Instant.now())) {
            return current.accessToken;
        }
        cachedToken = requestNewToken();
        return cachedToken.accessToken;
    }

    private CachedToken requestNewToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        TokenResponse response = restClient.post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Keycloak token endpoint returned no access_token for client "
                    + properties.clientId() + ".");
        }
        Instant expiresAt = Instant.now().plusSeconds(Math.max(response.expiresIn() - EXPIRY_SAFETY_MARGIN_SECONDS, 0));
        return new CachedToken(response.accessToken(), expiresAt);
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }
}
