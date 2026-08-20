package com.foodloop.matching.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.matching.application.MatchingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MatchProposalController {

    private final MatchingService matchingService;

    public MatchProposalController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /** The deterministic MatchingEngine's ranked candidate set (spec §17) — read-only, nothing persisted yet. */
    @GetMapping("/api/v1/matches/candidates")
    public List<MatchCandidateResponse> candidates(
            @RequestParam UUID foodListingId, @RequestParam(required = false) Double radiusKm) {
        return matchingService.findCandidates(currentTenantId(), foodListingId, radiusKm).stream()
                .map(MatchCandidateResponse::from)
                .toList();
    }

    @PostMapping("/api/v1/matches")
    public ResponseEntity<MatchProposalResponse> create(@Valid @RequestBody CreateMatchProposalRequest request) {
        var proposal = matchingService.createProposal(
                currentTenantId(), request.foodListingId(), request.receiverOrgId(), request.aiRationale(), request.ngoRequestId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MatchProposalResponse.from(proposal));
    }

    @GetMapping("/api/v1/matches")
    public List<MatchProposalResponse> listForListing(@RequestParam UUID foodListingId) {
        return matchingService.listForListing(foodListingId).stream()
                .map(MatchProposalResponse::from)
                .toList();
    }

    private UUID currentTenantId() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_NOT_RESOLVED", HttpStatus.BAD_REQUEST, "Request's JWT carried no tenant_id claim.");
        }
        return tenantId;
    }
}
