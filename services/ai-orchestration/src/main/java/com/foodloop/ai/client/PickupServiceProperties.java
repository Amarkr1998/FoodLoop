package com.foodloop.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodloop.services.pickup")
public record PickupServiceProperties(String baseUrl) {
}
