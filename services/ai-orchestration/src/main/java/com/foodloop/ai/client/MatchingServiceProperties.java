package com.foodloop.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodloop.services.matching")
public record MatchingServiceProperties(String baseUrl) {
}
