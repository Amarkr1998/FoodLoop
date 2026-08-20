package com.foodloop.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Minimal client-side view of Spring Data's default Page JSON — only the field this client actually reads. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageDto<T>(List<T> content) {
}
