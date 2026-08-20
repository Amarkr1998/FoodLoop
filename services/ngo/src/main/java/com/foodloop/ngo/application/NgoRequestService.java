package com.foodloop.ngo.application;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.ngo.domain.NgoRequest;
import com.foodloop.ngo.domain.NgoRequestRepository;
import com.foodloop.ngo.domain.NgoRequestStatus;
import com.foodloop.ngo.infrastructure.events.NgoEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NgoRequestService {

    private final NgoRequestRepository ngoRequestRepository;
    private final NgoEventPublisher eventPublisher;

    public NgoRequestService(NgoRequestRepository ngoRequestRepository, NgoEventPublisher eventPublisher) {
        this.ngoRequestRepository = ngoRequestRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public NgoRequest create(
            UUID tenantId, UUID ngoOrgId, String foodCategory, BigDecimal quantityNeeded, String quantityUnit,
            Instant neededBefore, String notes) {
        NgoRequest request = ngoRequestRepository.save(
                new NgoRequest(tenantId, ngoOrgId, foodCategory, quantityNeeded, quantityUnit, neededBefore, notes));
        eventPublisher.publishRequestCreated(request);
        return request;
    }

    @Transactional(readOnly = true)
    public NgoRequest get(UUID id) {
        return ngoRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException("NGO_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "NGO request " + id + " was not found."));
    }

    @Transactional(readOnly = true)
    public List<NgoRequest> listForOrg(UUID ngoOrgId) {
        return ngoRequestRepository.findByNgoOrgId(ngoOrgId);
    }

    /** The NGO Coordination Agent's scheduled sweep and on-demand trigger both read this (spec §19). */
    @Transactional(readOnly = true)
    public List<NgoRequest> listOpen() {
        return ngoRequestRepository.findByStatus(NgoRequestStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public List<NgoRequest> listOpenNearingDeadline(Instant cutoff) {
        return ngoRequestRepository.findByStatusAndNeededBeforeLessThanEqual(NgoRequestStatus.OPEN, cutoff);
    }

    @Transactional
    public NgoRequest cancel(UUID id) {
        NgoRequest request = get(id);
        request.cancel();
        return request;
    }

    @Transactional
    public NgoRequest markFulfilled(UUID id) {
        NgoRequest request = get(id);
        request.markFulfilled();
        return request;
    }

    /**
     * Consumed from match.proposed.v1 (MatchProposedListener), not an
     * authenticated HTTP caller — {@link TenantContext} is set explicitly
     * from the event's own tenantId, the same pattern Impact's
     * recordFromPickupCompleted uses. Idempotent, see NgoRequest#markMatched.
     */
    @Transactional
    public void markMatchedFromProposal(UUID tenantId, UUID ngoRequestId, UUID proposalId, UUID foodListingId) {
        TenantContext.set(tenantId);
        try {
            ngoRequestRepository.findById(ngoRequestId).ifPresent(request -> request.markMatched(proposalId, foodListingId));
        } finally {
            TenantContext.clear();
        }
    }
}
