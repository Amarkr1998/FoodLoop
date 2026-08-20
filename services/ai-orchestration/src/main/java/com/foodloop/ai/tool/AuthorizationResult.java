package com.foodloop.ai.tool;

public record AuthorizationResult(boolean allowed, String reason) {

    public static AuthorizationResult allow() {
        return new AuthorizationResult(true, null);
    }

    public static AuthorizationResult deny(String reason) {
        return new AuthorizationResult(false, reason);
    }
}
