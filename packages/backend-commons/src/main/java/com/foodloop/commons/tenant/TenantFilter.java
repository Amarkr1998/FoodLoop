package com.foodloop.commons.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
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
 *
 * <p>One exception: {@code foodloop-ai-orchestration}'s service-account JWT
 * has no {@code tenant_id} attribute of its own — a single client-credentials
 * principal acts on behalf of whichever tenant triggered a given agent run,
 * so it cannot have one fixed tenant baked into the token
 * (docs/architecture/05-ai-agent-architecture.md §1: agents call business
 * APIs through the same authenticated boundary a normal client would use).
 * For that one trusted client only — identified by the JWT's own signed
 * {@code azp} claim, never by anything the caller can freely set — an
 * explicit {@value #TENANT_HEADER} header supplies the tenant instead. No
 * other client is granted this, so an ordinary donor/receiver JWT (issued to
 * {@code foodloop-web}/{@code foodloop-mobile}) can never use the header to
 * impersonate a different tenant.
 */
public class TenantFilter extends OncePerRequestFilter {

    static final String TENANT_CLAIM = "tenant_id";
    static final String TENANT_HEADER = "X-Tenant-Id";
    static final String AZP_CLAIM = "azp";
    static final Set<String> DELEGATED_TENANT_HEADER_CLIENTS = Set.of("foodloop-ai-orchestration");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            resolveTenantId(request).ifPresent(TenantContext::set);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Optional<UUID> resolveTenantId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return Optional.empty();
        }
        Jwt jwt = jwtAuth.getToken();

        Optional<UUID> claimTenant = parseUuid(jwt.getClaimAsString(TENANT_CLAIM));
        if (claimTenant.isPresent()) {
            return claimTenant;
        }

        if (DELEGATED_TENANT_HEADER_CLIENTS.contains(jwt.getClaimAsString(AZP_CLAIM))) {
            return parseUuid(request.getHeader(TENANT_HEADER));
        }
        return Optional.empty();
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
