package com.foodloop.food.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    Optional<Claim> findByIdempotencyKey(String idempotencyKey);
}
