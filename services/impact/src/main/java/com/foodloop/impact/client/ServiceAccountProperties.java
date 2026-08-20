package com.foodloop.impact.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodloop.service-account")
public record ServiceAccountProperties(String clientId, String clientSecret, String tokenUri) {
}
