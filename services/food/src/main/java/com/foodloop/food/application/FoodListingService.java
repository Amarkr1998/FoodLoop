package com.foodloop.food.application;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.food.api.CreateFoodListingRequest;
import com.foodloop.food.domain.FoodAiMetadata;
import com.foodloop.food.domain.FoodListing;
import com.foodloop.food.domain.FoodListingRepository;
import com.foodloop.food.domain.FoodStatus;
import com.foodloop.food.domain.GeoUtils;
import com.foodloop.food.infrastructure.events.FoodEventPublisher;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FoodListingService {

    private static final Logger log = LoggerFactory.getLogger(FoodListingService.class);

    private final FoodListingRepository foodListingRepository;
    private final FoodEventPublisher eventPublisher;

    public FoodListingService(FoodListingRepository foodListingRepository, FoodEventPublisher eventPublisher) {
        this.foodListingRepository = foodListingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FoodListing createDraft(UUID tenantId, UUID donorUserId, CreateFoodListingRequest request) {
        double lat = request.latitude().doubleValue();
        double lng = request.longitude().doubleValue();
        FoodListing listing = new FoodListing(
                tenantId, request.donorOrgId(), donorUserId, request.title(), request.description(),
                request.foodCategory(),
                request.dietaryTypes() != null ? request.dietaryTypes() : java.util.List.of(),
                request.allergens() != null ? request.allergens() : java.util.List.of(),
                request.quantityValue(), request.quantityUnit(), request.estimatedServings(),
                request.preparationTime(), request.expiryTime(), request.pickupStartTime(), request.pickupEndTime(),
                GeoUtils.point(lat, lng), GeoUtils.jitter(lat, lng));
        return foodListingRepository.save(listing);
    }

    /**
     * DRAFT -> PUBLISHED -> AVAILABLE in one call: "publish" *is* "make
     * discoverable," so both state-machine hops (§11) happen together
     * rather than exposing PUBLISHED as a state a donor has to separately
     * advance past.
     */
    @Transactional
    public FoodListing publish(UUID id, UUID callerUserId) {
        FoodListing listing = getOwned(id, callerUserId);
        listing.transitionTo(FoodStatus.PUBLISHED);
        listing.transitionTo(FoodStatus.AVAILABLE);
        FoodListing saved = foodListingRepository.save(listing);
        eventPublisher.publishFoodListed(saved);
        return saved;
    }

    /**
     * Called only by the AI orchestration service's updateFoodListingAiMetadata
     * tool (a trusted service account, not a donor — see FoodListingController
     * and TenantFilter's delegated-tenant-header Javadoc), so there is no
     * ownership check here; tenant isolation (RLS) is still fully enforced via
     * the same {@link TenantContext} every other method relies on.
     */
    @Transactional
    public FoodListing applyAiMetadata(UUID id, FoodAiMetadata metadata) {
        FoodListing listing = get(id);
        listing.recordAiMetadata(metadata);
        return foodListingRepository.save(listing);
    }

    @Transactional
    public FoodListing cancel(UUID id, UUID callerUserId) {
        FoodListing listing = getOwned(id, callerUserId);
        listing.transitionTo(FoodStatus.CANCELLED);
        return foodListingRepository.save(listing);
    }

    @Transactional(readOnly = true)
    public FoodListing get(UUID id) {
        return foodListingRepository.findById(id)
                .orElseThrow(() -> new ApiException("FOOD_LISTING_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "No food listing found with id " + id + "."));
    }

    @Transactional(readOnly = true)
    public Page<FoodListing> searchNearby(
            UUID tenantId, double lat, double lng, double radiusKm, String category, String dietaryType, Pageable pageable) {
        return foodListingRepository.searchNearby(tenantId, lat, lng, radiusKm * 1000.0, category, dietaryType, pageable);
    }

    /**
     * Triggered by the Kafka listener consuming pickup.completed.v1, not an
     * authenticated HTTP caller — {@link TenantContext} is set explicitly
     * from the event's own tenantId (same pattern as identity's
     * RegistrationService and pickup's own createFromClaim). Advances
     * CLAIMED -> PICKUP_SCHEDULED -> PICKED_UP -> COMPLETED in one go, the
     * same "collapse the intermediate states into one event-driven hop"
     * approach {@link #publish} uses for DRAFT -> PUBLISHED -> AVAILABLE.
     */
    public void applyPickupCompleted(UUID tenantId, UUID foodListingId) {
        TenantContext.set(tenantId);
        try {
            FoodListing listing = get(foodListingId);
            listing.transitionTo(FoodStatus.PICKUP_SCHEDULED);
            listing.transitionTo(FoodStatus.PICKED_UP);
            listing.transitionTo(FoodStatus.COMPLETED);
            foodListingRepository.save(listing);
        } catch (ApiException e) {
            // Redelivery or a listing already advanced past this point —
            // logged, not rethrown: there is no HTTP caller here to return
            // an error to, and the alternative (crashing the consumer) would
            // just replay the same event forever.
            log.warn("Could not apply pickup completion to foodListingId={}: {}", foodListingId, e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    /** See {@link #applyPickupCompleted} — same reasoning, no-show path. */
    public void applyPickupNoShow(UUID tenantId, UUID foodListingId) {
        TenantContext.set(tenantId);
        try {
            FoodListing listing = get(foodListingId);
            listing.transitionTo(FoodStatus.PICKUP_SCHEDULED);
            listing.transitionTo(FoodStatus.NO_SHOW);
            foodListingRepository.save(listing);
        } catch (ApiException e) {
            log.warn("Could not apply pickup no-show to foodListingId={}: {}", foodListingId, e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private FoodListing getOwned(UUID id, UUID callerUserId) {
        FoodListing listing = get(id);
        if (!listing.getDonorUserId().equals(callerUserId)) {
            throw new ApiException("NOT_LISTING_OWNER", HttpStatus.FORBIDDEN,
                    "You are not authorized to act on food listing " + id + ".");
        }
        return listing;
    }
}
