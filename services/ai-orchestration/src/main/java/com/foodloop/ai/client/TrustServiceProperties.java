package com.foodloop.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodloop.services.trust")
public record TrustServiceProperties(String baseUrl) {
}
