package com.foodloop.pickup.application;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.commons.web.ApiException;
import com.foodloop.pickup.domain.GeoUtils;
import com.foodloop.pickup.domain.PickupTask;
import com.foodloop.pickup.domain.PickupTaskRepository;
import com.foodloop.pickup.domain.VolunteerProfile;
import com.foodloop.pickup.infrastructure.events.FoodClaimedEvent;
import com.foodloop.pickup.infrastructure.events.PickupEventPublisher;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickupService {

    private static final Logger log = LoggerFactory.getLogger(PickupService.class);

    private final PickupTaskRepository pickupTaskRepository;
    private final PickupEventPublisher eventPublisher;
    private final VolunteerService volunteerService;

    public PickupService(
            PickupTaskRepository pickupTaskRepository, PickupEventPublisher eventPublisher, VolunteerService volunteerService) {
        this.pickupTaskRepository = pickupTaskRepository;
        this.eventPublisher = eventPublisher;
        this.volunteerService = volunteerService;
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

    /** Either the donor or the assigned volunteer (once one exists) may confirm completion. */
    @Transactional
    public PickupTask complete(UUID id, UUID callerUserId) {
        PickupTask task = getOwnedByDonorOrVolunteer(id, callerUserId);
        task.complete();
        PickupTask saved = pickupTaskRepository.save(task);
        eventPublisher.publishPickupCompleted(saved);
        return saved;
    }

    @Transactional
    public PickupTask reportNoShow(UUID id, UUID callerUserId) {
        PickupTask task = getOwnedByDonorOrVolunteer(id, callerUserId);
        task.reportNoShow();
        PickupTask saved = pickupTaskRepository.save(task);
        eventPublisher.publishPickupNoShow(saved);
        return saved;
    }

    /** Donor or receiver opts into volunteer-mediated pickup instead of the direct handoff (spec Phase 10). */
    @Transactional
    public PickupTask requestVolunteer(UUID id, UUID callerUserId) {
        PickupTask task = getOwnedByDonorOrReceiver(id, callerUserId);
        task.requestVolunteer();
        return pickupTaskRepository.save(task);
    }

    /**
     * A registered volunteer claims an UNASSIGNED task — same self-service
     * pattern as Food's claim, no automatic/AI matching in this phase
     * (that's the deferred Pickup Agent's job, spec §20).
     */
    @Transactional
    public PickupTask claimAsVolunteer(UUID id, UUID callerUserId) {
        VolunteerProfile volunteer = volunteerService.getByUserId(callerUserId);
        PickupTask task = get(id);
        task.assignVolunteer(volunteer.getUserId());
        return pickupTaskRepository.save(task);
    }

    @Transactional
    public PickupTask volunteerEnRoute(UUID id, UUID callerUserId) {
        PickupTask task = getOwnedByVolunteer(id, callerUserId);
        task.volunteerEnRoute();
        return pickupTaskRepository.save(task);
    }

    @Transactional
    public PickupTask volunteerArrived(UUID id, UUID callerUserId) {
        PickupTask task = getOwnedByVolunteer(id, callerUserId);
        task.volunteerArrived();
        return pickupTaskRepository.save(task);
    }

    /** The assigned volunteer backs out — the task returns to the open pool for another volunteer to claim. */
    @Transactional
    public PickupTask unassignVolunteer(UUID id, UUID callerUserId) {
        PickupTask task = getOwnedByVolunteer(id, callerUserId);
        task.unassignVolunteer();
        return pickupTaskRepository.save(task);
    }

    /** What a volunteer browses to find work (spec Phase 10) — mirrors Food's public nearby search. */
    @Transactional(readOnly = true)
    public Page<PickupTask> searchAvailableForVolunteers(UUID tenantId, double lat, double lng, double radiusKm, Pageable pageable) {
        return pickupTaskRepository.searchNearbyUnassigned(tenantId, lat, lng, radiusKm * 1000.0, pageable);
    }

    private PickupTask getOwnedByDonorOrReceiver(UUID id, UUID callerUserId) {
        PickupTask task = get(id);
        if (!task.getDonorUserId().equals(callerUserId) && !task.getReceiverUserId().equals(callerUserId)) {
            throw new ApiException("NOT_PICKUP_PARTICIPANT", HttpStatus.FORBIDDEN,
                    "You are not authorized to act on pickup task " + id + ".");
        }
        return task;
    }

    private PickupTask getOwnedByDonorOrVolunteer(UUID id, UUID callerUserId) {
        PickupTask task = get(id);
        boolean isDonor = task.getDonorUserId().equals(callerUserId);
        boolean isAssignedVolunteer = callerUserId.equals(task.getAssignedVolunteerId());
        if (!isDonor && !isAssignedVolunteer) {
            throw new ApiException("NOT_PICKUP_OWNER", HttpStatus.FORBIDDEN,
                    "You are not authorized to act on pickup task " + id + ".");
        }
        return task;
    }

    private PickupTask getOwnedByVolunteer(UUID id, UUID callerUserId) {
        PickupTask task = get(id);
        if (!callerUserId.equals(task.getAssignedVolunteerId())) {
            throw new ApiException("NOT_ASSIGNED_VOLUNTEER", HttpStatus.FORBIDDEN,
                    "You are not the volunteer assigned to pickup task " + id + ".");
        }
        return task;
    }
}
