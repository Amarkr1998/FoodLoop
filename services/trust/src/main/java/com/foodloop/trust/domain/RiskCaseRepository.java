package com.foodloop.trust.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskCaseRepository extends JpaRepository<RiskCase, UUID> {

    List<RiskCase> findByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);
}
