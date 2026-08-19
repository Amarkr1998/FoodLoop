package com.foodloop.identity.infrastructure.keycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The identity service authenticates to Keycloak's Admin REST API as its own
 * confidential client ({@code foodloop-identity}, service account granted
 * realm-management's {@code manage-users}/{@code view-users} roles — see
 * infrastructure/docker/keycloak/foodloop-realm.json), not as an end user.
 * This is the only place in the platform that creates Keycloak accounts.
 */
@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfig {

    @Bean
    public Keycloak keycloakAdminClient(KeycloakAdminProperties properties) {
        return KeycloakBuilder.builder()
                .serverUrl(properties.serverUrl())
                .realm(properties.realm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(properties.adminClientId())
                .clientSecret(properties.adminClientSecret())
                .build();
    }
}
