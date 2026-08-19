package com.foodloop.identity.config;

import com.foodloop.commons.tenant.TenantFilter;
import com.foodloop.commons.web.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

/**
 * Independently validates the caller's JWT even though the API gateway
 * already does so at the edge — defense in depth per ADR-007/the threat
 * model (T5): this service must never trust an unauthenticated or
 * gateway-bypassed request. Registration is the one public endpoint, since
 * there is by definition no account yet to authenticate.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TenantFilter tenantFilter;
    private final CorrelationIdFilter correlationIdFilter;

    public SecurityConfig(TenantFilter tenantFilter, CorrelationIdFilter correlationIdFilter) {
        this.tenantFilter = tenantFilter;
        this.correlationIdFilter = correlationIdFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .addFilterBefore(correlationIdFilter, WebAsyncManagerIntegrationFilter.class)
                .addFilterAfter(tenantFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }
}
