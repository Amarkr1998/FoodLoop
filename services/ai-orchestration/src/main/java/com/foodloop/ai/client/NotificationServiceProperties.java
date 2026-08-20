package com.foodloop.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodloop.services.notification")
public record NotificationServiceProperties(String baseUrl) {
}
