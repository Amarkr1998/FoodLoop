package com.foodloop.food.api;

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
        FoodListing listing = foodListingService.createDraft(tenantId(authentication), callerUserId(authentication), request);
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
    public List<FoodListingResponse> expiring(
            JwtAuthenticationToken authentication, @RequestParam int withinMinutes) {
        return foodListingService.findExpiringSoon(tenantId(authentication), withinMinutes).stream()
                .map(FoodListingResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/food-listings")
    public Page<FoodListingResponse> search(
            JwtAuthenticationToken authentication,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String dietaryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return foodListingService
                .searchNearby(tenantId(authentication), lat, lng, radiusKm, category, dietaryType, pageable)
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

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }

    private UUID tenantId(JwtAuthenticationToken authentication) {
        String tenantClaim = authentication.getToken().getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return UUID.fromString(tenantClaim);
    }
}
