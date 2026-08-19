package com.foodloop.commons.web;

/**
 * The platform-wide error envelope (docs/architecture/03-api-catalog.md, §42).
 * Every 4xx/5xx response from every service uses this shape; internal
 * exception detail never reaches the client.
 */
public record ApiError(String code, String message, String traceId) {
}
