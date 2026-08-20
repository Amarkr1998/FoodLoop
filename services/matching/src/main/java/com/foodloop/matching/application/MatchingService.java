package com.foodloop.matching.application;

import com.foodloop.commons.web.ApiException;
import com.foodloop.matching.client.FoodListingDto;
import com.foodloop.matching.client.FoodServiceClient;
import com.foodloop.matching.client.OrganizationDto;
import com.foodloop.matching.client.TenantServiceClient;
import com.foodloop.matching.domain.DistanceCalculator;
import com.foodloop.matching.domain.MatchProposal;
import com.foodloop.matching.domain.MatchProposalRepository;
import com.foodloop.matching.domain.MatchStatus;
import com.foodloop.matching.domain.MatchingEngine;
import com.foodloop.matching.infrastructure.events.MatchEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic candidate search + server-side re-validated proposal
 * creation (spec §17). {@link #createProposal} re-fetches the listing and
 * org from their owning services and re-derives distance/score itself — it
 * never trusts a distance or score the caller supplies — so a
 * prompt-injected agent can choose *which* candidate to propose but cannot
 * fabricate *why* it was eligible (docs/architecture/05-ai-agent-architecture.md
 * §4: "tool-side validation, not just prompt trust").
 */
@Service
public class MatchingService {

    /** Mirrors OrganizationRepository#searchNearbyReceivers's hardcoded type list (services/tenant) — re-checked here, not assumed, since getOrganization doesn't filter by type at all. */
    private static final Set<String> RECEIVER_CAPABLE_TYPES = Set.of("NGO", "FOOD_BANK", "CORPORATE", "INDIVIDUAL");
    private static final double DEFAULT_CANDIDATE_RADIUS_KM = 10.0;
    private static final double MAX_PROPOSAL_RADIUS_METERS = 25_000.0;

    private final MatchProposalRepository matchProposalRepository;
    private final FoodServiceClient foodServiceClient;
    private final TenantServiceClient tenantServiceClient;
    private final MatchEventPublisher eventPublisher;

    public MatchingService(
            MatchProposalRepository matchProposalRepository,
            FoodServiceClient foodServiceClient,
            TenantServiceClient tenantServiceClient,
            MatchEventPublisher eventPublisher) {
        this.matchProposalRepository = matchProposalRepository;
        this.foodServiceClient = foodServiceClient;
        this.tenantServiceClient = tenantServiceClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<MatchCandidate> findCandidates(UUID tenantId, UUID foodListingId, Double radiusKmOrNull) {
        double radiusKm = radiusKmOrNull != null ? radiusKmOrNull : DEFAULT_CANDIDATE_RADIUS_KM;
        FoodListingDto listing = requireAvailableListing(tenantId, foodListingId);
        double radiusMeters = radiusKm * 1000.0;
        Instant now = Instant.now();

        return tenantServiceClient.searchNearbyReceivers(tenantId, listing.latitude(), listing.longitude(), radiusKm, null).stream()
                .filter(org -> org.latitude() != null && org.longitude() != null)
                .map(org -> {
                    double distance = DistanceCalculator.distanceMeters(
                            listing.latitude(), listing.longitude(), org.latitude(), org.longitude());
                    BigDecimal score = MatchingEngine.score(distance, radiusMeters, listing.expiryTime(), now);
                    return new MatchCandidate(org.id(), org.name(), distance, score);
                })
                .sorted(Comparator.comparing(MatchCandidate::score).reversed())
                .toList();
    }

    @Transactional
    public MatchProposal createProposal(UUID tenantId, UUID foodListingId, UUID receiverOrgId, String aiRationale) {
        return createProposal(tenantId, foodListingId, receiverOrgId, aiRationale, null);
    }

    @Transactional
    public MatchProposal createProposal(
            UUID tenantId, UUID foodListingId, UUID receiverOrgId, String aiRationale, UUID ngoRequestId) {
        FoodListingDto listing = requireAvailableListing(tenantId, foodListingId);

        OrganizationDto org = tenantServiceClient.getOrganization(tenantId, receiverOrgId);
        if (org == null || !RECEIVER_CAPABLE_TYPES.contains(org.type())) {
            throw new ApiException("ORG_NOT_RECEIVER_CAPABLE", HttpStatus.CONFLICT,
                    "Organization " + receiverOrgId + " is not a receiver-capable type.");
        }
        if (org.latitude() == null || org.longitude() == null) {
            throw new ApiException("ORG_HAS_NO_LOCATION", HttpStatus.CONFLICT,
                    "Organization " + receiverOrgId + " has not set a location.");
        }

        double distanceMeters = DistanceCalculator.distanceMeters(
                listing.latitude(), listing.longitude(), org.latitude(), org.longitude());
        if (distanceMeters > MAX_PROPOSAL_RADIUS_METERS) {
            throw new ApiException("ORG_TOO_FAR", HttpStatus.CONFLICT,
                    "Organization " + receiverOrgId + " is outside the maximum match radius.");
        }
        if (matchProposalRepository.existsByFoodListingIdAndReceiverOrgIdAndStatus(
                foodListingId, receiverOrgId, MatchStatus.PROPOSED)) {
            throw new ApiException("MATCH_ALREADY_PROPOSED", HttpStatus.CONFLICT,
                    "A proposal for this listing and organization is already open.");
        }

        BigDecimal score = MatchingEngine.score(distanceMeters, MAX_PROPOSAL_RADIUS_METERS, listing.expiryTime(), Instant.now());
        MatchProposal proposal = matchProposalRepository.save(new MatchProposal(
                tenantId, foodListingId, receiverOrgId, BigDecimal.valueOf(distanceMeters), score, aiRationale, ngoRequestId));
        eventPublisher.publishMatchProposed(proposal);
        return proposal;
    }

    @Transactional(readOnly = true)
    public List<MatchProposal> listForListing(UUID foodListingId) {
        return matchProposalRepository.findByFoodListingId(foodListingId);
    }

    private FoodListingDto requireAvailableListing(UUID tenantId, UUID foodListingId) {
        FoodListingDto listing = foodServiceClient.getFoodListing(tenantId, foodListingId);
        if (listing == null || !"AVAILABLE".equals(listing.status())) {
            throw new ApiException("LISTING_NOT_AVAILABLE", HttpStatus.CONFLICT,
                    "Food listing " + foodListingId + " is not AVAILABLE.");
        }
        return listing;
    }
}
