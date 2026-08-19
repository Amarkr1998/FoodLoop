package com.foodloop.identity.infrastructure.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodloop.keycloak")
public record KeycloakAdminProperties(
        String serverUrl,
        String realm,
        String adminClientId,
        String adminClientSecret) {
}
