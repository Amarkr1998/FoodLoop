package com.foodloop.matching.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchProposalRepository extends JpaRepository<MatchProposal, UUID> {

    List<MatchProposal> findByFoodListingId(UUID foodListingId);

    boolean existsByFoodListingIdAndReceiverOrgIdAndStatus(UUID foodListingId, UUID receiverOrgId, MatchStatus status);
}
