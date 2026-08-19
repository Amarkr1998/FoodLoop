package com.foodloop.identity.api;

public record UpdateProfileRequest(String displayName, String phone, String locale) {
}
