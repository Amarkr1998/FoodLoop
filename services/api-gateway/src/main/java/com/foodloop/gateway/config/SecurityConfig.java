package com.foodloop.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Edge authentication: every request except health/docs must carry a valid
 * Keycloak-issued JWT (docs/architecture/06-security-threat-model.md, §30).
 * This is coarse edge enforcement only — each downstream service still
 * independently validates the token and cross-checks tenant/role claims
 * against its own data (defense in depth per ADR-009); the gateway does not
 * become the sole authorization boundary.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                        .pathMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
