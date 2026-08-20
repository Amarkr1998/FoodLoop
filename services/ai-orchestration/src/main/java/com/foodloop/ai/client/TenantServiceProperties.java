package com.foodloop.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodloop.services.tenant")
public record TenantServiceProperties(String baseUrl) {
}
