package com.foodloop.commons.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Unit coverage for the trusted-header delegated-tenant path (see the
 * Javadoc on {@link TenantFilter}) — no real request ever reaches
 * {@link TenantAwareDataSource}/RLS here, only the resolution logic that
 * decides what to stamp {@link TenantContext} with.
 */
class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void usesTenantClaimWhenPresent() throws Exception {
        UUID tenantId = UUID.randomUUID();
        authenticateAs(jwt("foodloop-web", Map.of("tenant_id", tenantId.toString())));

        FilterChain chain = capturingChain(tenantId);
        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(TenantContext.get()).isNull(); // cleared after the filter completes
    }

    @Test
    void trustedAiOrchestrationClientCanDelegateTenantViaHeader() throws Exception {
        UUID tenantId = UUID.randomUUID();
        authenticateAs(jwt("foodloop-ai-orchestration", Map.of()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", tenantId.toString());

        filter.doFilterInternal(request, new MockHttpServletResponse(), capturingChain(tenantId));
    }

    @Test
    void untrustedClientCannotDelegateTenantViaHeader() throws Exception {
        UUID tenantId = UUID.randomUUID();
        authenticateAs(jwt("foodloop-web", Map.of()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", tenantId.toString());

        filter.doFilterInternal(request, new MockHttpServletResponse(), capturingChain(null));
    }

    @Test
    void noAuthenticationLeavesTenantUnset() throws Exception {
        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), capturingChain(null));
    }

    private void authenticateAs(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private Jwt jwt(String azp, Map<String, Object> extraClaims) {
        var claims = new java.util.HashMap<String, Object>(extraClaims);
        claims.put("azp", azp);
        claims.put("sub", UUID.randomUUID().toString());
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    /** Asserts, at the moment the request is actually processed, what TenantContext holds. */
    private FilterChain capturingChain(UUID expectedTenant) {
        FilterChain chain = mock(FilterChain.class);
        try {
            org.mockito.Mockito.doAnswer(invocation -> {
                        assertThat(TenantContext.get()).isEqualTo(expectedTenant);
                        return null;
                    })
                    .when(chain)
                    .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return chain;
    }
}
