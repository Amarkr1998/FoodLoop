package com.foodloop.food.config;

import com.foodloop.commons.tenant.TenantFilter;
import com.foodloop.commons.web.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;

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
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .addFilterBefore(correlationIdFilter, WebAsyncManagerIntegrationFilter.class)
                .addFilterAfter(tenantFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }
}
