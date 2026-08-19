package com.foodloop.food.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.food.domain.Claim;
import com.foodloop.food.domain.ClaimRepository;
import com.foodloop.food.domain.FoodListing;
import com.foodloop.food.domain.FoodListingRepository;
import com.foodloop.food.domain.FoodStatus;
import com.foodloop.food.infrastructure.events.FoodEventPublisher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Race-condition-safe claiming (spec §13, §46: "test critical race
 * conditions such as two users claiming the same food simultaneously").
 * Two layers, both database-enforced, not just checked in application code:
 *
 * <ol>
 *   <li>Optimistic locking on {@link FoodListing#getVersion()} — two
 *   concurrent transactions that both read the listing as AVAILABLE will
 *   both attempt {@code UPDATE ... WHERE id=? AND version=?}; only one
 *   affects a row. {@link #claim} forces an immediate flush
 *   ({@code saveAndFlush}) specifically so that failure surfaces inside
 *   this method's own try/catch instead of at transaction commit, after
 *   the method has already returned.</li>
 *   <li>The partial unique index on {@code food.claim(food_listing_id)
 *   WHERE status IN ('PENDING','CONFIRMED')} (V1 migration) is the
 *   backstop in case the first layer is ever bypassed.</li>
 * </ol>
 */
@Service
public class ClaimService {

    private static final int CLAIM_EXPIRY_HOURS = 4;

    private final FoodListingRepository foodListingRepository;
    private final ClaimRepository claimRepository;
    private final FoodEventPublisher eventPublisher;

    public ClaimService(
            FoodListingRepository foodListingRepository, ClaimRepository claimRepository, FoodEventPublisher eventPublisher) {
        this.foodListingRepository = foodListingRepository;
        this.claimRepository = claimRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Claim claim(UUID foodListingId, UUID callerUserId, UUID receiverOrgId, String idempotencyKey) {
        var existing = claimRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        FoodListing listing = foodListingRepository.findById(foodListingId)
                .orElseThrow(() -> new ApiException("FOOD_LISTING_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "No food listing found with id " + foodListingId + "."));

        // Checked explicitly (not just left to transitionTo's generic guard)
        // so a claim attempt on an already-claimed listing gets the specific
        // FOOD_ALREADY_CLAIMED code a client can branch on, rather than the
        // generic INVALID_STATE_TRANSITION every other illegal transition uses.
        if (listing.getStatus() != FoodStatus.AVAILABLE) {
            throw new ApiException("FOOD_ALREADY_CLAIMED", HttpStatus.CONFLICT,
                    "This food listing is no longer available.");
        }
        listing.transitionTo(FoodStatus.CLAIMED);

        try {
            foodListingRepository.saveAndFlush(listing);
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
            // The race this guards against: another transaction claimed it
            // between our read above and this flush (§13, §46) — the
            // in-memory check just above can't see that, only the database
            // constraint/version check can.
            throw new ApiException("FOOD_ALREADY_CLAIMED", HttpStatus.CONFLICT,
                    "This food listing is no longer available.");
        }

        Claim claimRecord = new Claim(
                listing.getTenantId(), foodListingId, callerUserId, receiverOrgId, idempotencyKey,
                Instant.now().plus(CLAIM_EXPIRY_HOURS, ChronoUnit.HOURS));
        try {
            Claim saved = claimRepository.saveAndFlush(claimRecord);
            eventPublisher.publishFoodClaimed(listing, saved);
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new ApiException("FOOD_ALREADY_CLAIMED", HttpStatus.CONFLICT,
                    "This food listing is no longer available.");
        }
    }
}
