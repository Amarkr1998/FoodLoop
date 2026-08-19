package com.foodloop.commons.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates {@link TenantContext} from the authenticated request's JWT
 * {@code tenant_id} claim (a client-level protocol mapper in
 * infrastructure/docker/keycloak/foodloop-realm.json, reading the user's
 * {@code tenant_id} attribute) so {@link TenantAwareDataSource} can enforce
 * it at the connection layer, and clears it once the request completes so a
 * pooled worker thread never leaks one request's tenant into the next.
 *
 * <p>Must be wired into each service's Spring Security filter chain
 * <em>after</em> the JWT authentication filter so the {@link Authentication}
 * is already populated, e.g.:
 * {@code http.addFilterAfter(tenantFilter, BearerTokenAuthenticationFilter.class)}.
 * A request whose JWT carries no {@code tenant_id} claim proceeds with no
 * tenant set — RLS policies then return zero rows rather than leaking data,
 * so a missing claim fails closed, not open.
 */
public class TenantFilter extends OncePerRequestFilter {

    static final String TENANT_CLAIM = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            resolveTenantId().ifPresent(TenantContext::set);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private java.util.Optional<UUID> resolveTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return java.util.Optional.empty();
        }
        Jwt jwt = jwtAuth.getToken();
        String tenantClaim = jwt.getClaimAsString(TENANT_CLAIM);
        if (tenantClaim == null || tenantClaim.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(UUID.fromString(tenantClaim));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }
}
