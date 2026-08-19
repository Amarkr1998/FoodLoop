package com.foodloop.pickup.application;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.pickup.domain.GeoUtils;
import com.foodloop.pickup.domain.PickupTask;
import com.foodloop.pickup.domain.PickupTaskRepository;
import com.foodloop.pickup.infrastructure.events.FoodClaimedEvent;
import com.foodloop.pickup.infrastructure.events.PickupEventPublisher;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickupService {

    private static final Logger log = LoggerFactory.getLogger(PickupService.class);

    private final PickupTaskRepository pickupTaskRepository;
    private final PickupEventPublisher eventPublisher;

    public PickupService(PickupTaskRepository pickupTaskRepository, PickupEventPublisher eventPublisher) {
        this.pickupTaskRepository = pickupTaskRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Triggered by the Kafka listener consuming food.claimed.v1, not an
     * authenticated HTTP caller — {@link TenantContext} is set explicitly
     * from the event's own tenantId, the same pattern identity's
     * RegistrationService uses for its one similarly caller-less write.
     * Idempotent on claim_id (unique index, V1 migration) so a redelivered
     * event never creates a second task (§7).
     */
    public void createFromClaim(FoodClaimedEvent event) {
        TenantContext.set(event.tenantId());
        try {
            if (pickupTaskRepository.findByClaimId(event.claimId()).isPresent()) {
                log.info("Pickup task already exists for claimId={}, skipping (idempotent redelivery)", event.claimId());
                return;
            }
            PickupTask task = new PickupTask(
                    event.tenantId(), event.claimId(), event.foodListingId(), event.donorUserId(), event.receiverUserId(),
                    event.pickupStartTime(), event.pickupEndTime(),
                    GeoUtils.point(event.latitude(), event.longitude()));
            pickupTaskRepository.save(task);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public PickupTask get(UUID id) {
        return pickupTaskRepository.findById(id)
                .orElseThrow(() -> new ApiException("PICKUP_TASK_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "No pickup task found with id " + id + "."));
    }

    @Transactional
    public PickupTask complete(UUID id, UUID callerUserId) {
        PickupTask task = getOwnedByDonor(id, callerUserId);
        task.complete();
        PickupTask saved = pickupTaskRepository.save(task);
        eventPublisher.publishPickupCompleted(saved);
        return saved;
    }

    @Transactional
    public PickupTask reportNoShow(UUID id, UUID callerUserId) {
        PickupTask task = getOwnedByDonor(id, callerUserId);
        task.reportNoShow();
        PickupTask saved = pickupTaskRepository.save(task);
        eventPublisher.publishPickupNoShow(saved);
        return saved;
    }

    private PickupTask getOwnedByDonor(UUID id, UUID callerUserId) {
        PickupTask task = get(id);
        if (!task.getDonorUserId().equals(callerUserId)) {
            throw new ApiException("NOT_PICKUP_OWNER", HttpStatus.FORBIDDEN,
                    "You are not authorized to act on pickup task " + id + ".");
        }
        return task;
    }
}
