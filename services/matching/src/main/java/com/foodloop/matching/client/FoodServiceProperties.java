package com.foodloop.matching.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "foodloop.services.food")
public record FoodServiceProperties(String baseUrl) {
}
