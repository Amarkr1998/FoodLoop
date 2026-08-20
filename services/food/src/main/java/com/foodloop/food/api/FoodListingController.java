package com.foodloop.food.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.food.application.ClaimService;
import com.foodloop.food.application.FoodListingService;
import com.foodloop.food.domain.Claim;
import com.foodloop.food.domain.FoodAiMetadata;
import com.foodloop.food.domain.FoodListing;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FoodListingController {

    /** Only this Keycloak client's service account may write AI metadata — see TenantFilter's Javadoc. */
    private static final String AI_ORCHESTRATION_CLIENT_ID = "foodloop-ai-orchestration";

    private final FoodListingService foodListingService;
    private final ClaimService claimService;

    public FoodListingController(FoodListingService foodListingService, ClaimService claimService) {
        this.foodListingService = foodListingService;
        this.claimService = claimService;
    }

    @PostMapping("/api/v1/food-listings")
    public ResponseEntity<FoodListingResponse> create(
            JwtAuthenticationToken authentication, @Valid @RequestBody CreateFoodListingRequest request) {
        FoodListing listing = foodListingService.createDraft(tenantId(), callerUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(FoodListingResponse.from(listing));
    }

    @PostMapping("/api/v1/food-listings/{id}/publish")
    public FoodListingResponse publish(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return FoodListingResponse.from(foodListingService.publish(id, callerUserId(authentication)));
    }

    /**
     * Written only by the AI orchestration service's updateFoodListingAiMetadata
     * tool, never by a donor directly — enforced by checking the JWT's own
     * signed {@code azp} claim, the same trust primitive
     * {@link com.foodloop.commons.tenant.TenantFilter} uses for the delegated
     * tenant header this same caller relies on to reach this endpoint at all.
     */
    @PutMapping("/api/v1/food-listings/{id}/ai-metadata")
    public FoodListingResponse updateAiMetadata(
            JwtAuthenticationToken authentication, @PathVariable UUID id, @Valid @RequestBody UpdateAiMetadataRequest request) {
        requireAiOrchestrationCaller(authentication);
        FoodAiMetadata metadata = new FoodAiMetadata(
                request.category(), request.dietaryTypes(), request.allergens(), request.estimatedServings(),
                request.urgency(), request.missingInformation(), request.suggestedDescription(), request.confidence(),
                Instant.now());
        return FoodListingResponse.from(foodListingService.applyAiMetadata(id, metadata));
    }

    /**
     * Written only by the AI orchestration service's flagForSafetyReview
     * tool — same {@code azp} trust check as updateAiMetadata. The write
     * itself is deliberately one-directional (see FoodListing#flagForSafetyReview):
     * an agent can raise a hold, never clear one.
     */
    @PutMapping("/api/v1/food-listings/{id}/safety-flag")
    public FoodListingResponse flagForSafetyReview(
            JwtAuthenticationToken authentication, @PathVariable UUID id, @Valid @RequestBody FlagSafetyReviewRequest request) {
        requireAiOrchestrationCaller(authentication);
        return FoodListingResponse.from(foodListingService.flagForSafetyReview(id, request.reason()));
    }

    /**
     * The one way a safety hold is lifted — gated on the caller's own
     * signed realm role (Keycloak's {@code realm_access.roles} claim,
     * standard OIDC shape), not an org-membership check like the donor
     * endpoints above: reviewing a safety flag isn't the donor's call to
     * make, whether or not they own the listing.
     */
    @PostMapping("/api/v1/food-listings/{id}/safety-flag/clear")
    public FoodListingResponse clearSafetyReview(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        requireTrustOpsCaller(authentication);
        return FoodListingResponse.from(foodListingService.clearSafetyReview(id));
    }

    @PostMapping("/api/v1/food-listings/{id}/cancel")
    public FoodListingResponse cancel(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return FoodListingResponse.from(foodListingService.cancel(id, callerUserId(authentication)));
    }

    @GetMapping("/api/v1/food-listings/{id}")
    public FoodListingResponse get(@PathVariable UUID id) {
        return FoodListingResponse.from(foodListingService.get(id));
    }

    /** The Food Rescue Agent's expiry sweep calls this per tenant per threshold (spec §18). */
    @GetMapping("/api/v1/food-listings/expiring")
    public List<FoodListingResponse> expiring(@RequestParam int withinMinutes) {
        return foodListingService.findExpiringSoon(tenantId(), withinMinutes).stream()
                .map(FoodListingResponse::from)
                .toList();
    }

    /** Also the NGO Coordination Agent's searchNearbyFood tool (spec §19) — same delegated-caller path as expiring above. */
    @GetMapping("/api/v1/food-listings")
    public Page<FoodListingResponse> search(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String dietaryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return foodListingService
                .searchNearby(tenantId(), lat, lng, radiusKm, category, dietaryType, pageable)
                .map(FoodListingResponse::fromPublic);
    }

    @PostMapping("/api/v1/food-listings/{id}/claim")
    public ResponseEntity<ClaimResponse> claim(
            JwtAuthenticationToken authentication,
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) ClaimRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException("IDEMPOTENCY_KEY_REQUIRED", HttpStatus.BAD_REQUEST,
                    "The Idempotency-Key header is required for claiming.");
        }
        UUID receiverOrgId = request != null ? request.receiverOrgId() : null;
        Claim claim = claimService.claim(id, callerUserId(authentication), receiverOrgId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClaimResponse.from(claim));
    }

    // Package-private (not private) so FoodListingControllerAiMetadataAuthorizationTest
    // can exercise this security-critical check directly.
    void requireAiOrchestrationCaller(JwtAuthenticationToken authentication) {
        String azp = authentication.getToken().getClaimAsString("azp");
        if (!AI_ORCHESTRATION_CLIENT_ID.equals(azp)) {
            throw new ApiException("FORBIDDEN_AI_METADATA_WRITE", HttpStatus.FORBIDDEN,
                    "Only the AI orchestration service may write AI metadata.");
        }
    }

    /**
     * Reads Keycloak's standard {@code realm_access.roles} claim directly
     * off the JWT — no service in this platform has needed a realm-role
     * check before this endpoint, so there's no shared helper for it yet in
     * backend-commons; adding one there ahead of a second caller would be
     * the premature abstraction the project's own conventions warn against.
     */
    void requireTrustOpsCaller(JwtAuthenticationToken authentication) {
        Object realmAccess = authentication.getToken().getClaims().get("realm_access");
        List<String> roles = (realmAccess instanceof java.util.Map<?, ?> map && map.get("roles") instanceof List<?> rawRoles)
                ? rawRoles.stream().map(String::valueOf).toList()
                : List.of();
        if (!roles.contains("TRUST_OPS") && !roles.contains("ADMIN")) {
            throw new ApiException("FORBIDDEN_SAFETY_REVIEW_CLEAR", HttpStatus.FORBIDDEN,
                    "Only TRUST_OPS or ADMIN may clear a safety review hold.");
        }
    }

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }

    /**
     * {@link TenantContext} (set by {@code TenantFilter} before this
     * controller runs) rather than re-parsing the JWT's {@code tenant_id}
     * claim directly: the claim is null for a trusted service-account caller
     * (e.g. the Rescue/NGO Coordination agents' delegated-tenant-header
     * calls — see TenantFilter's Javadoc), and TenantContext already
     * resolves both that case and the ordinary human-JWT-claim case
     * uniformly, so re-deriving it here a second, narrower way would just
     * reintroduce the gap TenantFilter exists to close.
     */
    private UUID tenantId() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return tenantId;
    }
}
