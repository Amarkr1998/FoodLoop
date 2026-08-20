package com.foodloop.ngo.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NgoRequestRepository extends JpaRepository<NgoRequest, UUID> {

    List<NgoRequest> findByNgoOrgId(UUID ngoOrgId);

    List<NgoRequest> findByStatus(NgoRequestStatus status);

    /** The NGO Coordination Agent's scheduled sweep (spec §19): open requests nearing their deadline. */
    List<NgoRequest> findByStatusAndNeededBeforeLessThanEqual(NgoRequestStatus status, Instant cutoff);

    Optional<NgoRequest> findByMatchedProposalId(UUID matchedProposalId);
}
