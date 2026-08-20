package com.foodloop.matching.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrganizationDto(UUID id, String name, String type, Double latitude, Double longitude) {
}
