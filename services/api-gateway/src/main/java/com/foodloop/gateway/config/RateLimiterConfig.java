package com.foodloop.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

/**
 * Redis-backed request rate limiting (docs/architecture/06-security-threat-model.md, T9).
 * Keyed on the authenticated subject when a JWT is present, falling back to
 * remote address for unauthenticated endpoints, so one client can't exhaust
 * another's quota. Wired to each route's RequestRateLimiter filter as routes
 * are added per phase (see pom.xml module comment).
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver rateLimitKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)
                .map(auth -> auth.getToken())
                .map(Jwt::getSubject)
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                        .map(addr -> addr.getAddress().getHostAddress()))
                .defaultIfEmpty("anonymous");
    }
}
