package com.foodloop.impact.application;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.impact.client.FoodListingDto;
import com.foodloop.impact.client.FoodServiceClient;
import com.foodloop.impact.domain.ImpactCalculator;
import com.foodloop.impact.domain.ImpactSummary;
import com.foodloop.impact.domain.RescueRecord;
import com.foodloop.impact.domain.RescueRecordRepository;
import com.foodloop.impact.infrastructure.events.PickupCompletedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImpactService {

    private static final Logger log = LoggerFactory.getLogger(ImpactService.class);

    private final RescueRecordRepository rescueRecordRepository;
    private final FoodServiceClient foodServiceClient;

    public ImpactService(RescueRecordRepository rescueRecordRepository, FoodServiceClient foodServiceClient) {
        this.rescueRecordRepository = rescueRecordRepository;
        this.foodServiceClient = foodServiceClient;
    }

    /**
     * Triggered by the Kafka listener consuming pickup.completed.v1, not an
     * authenticated HTTP caller — {@link TenantContext} is set explicitly
     * from the event's own tenantId, the same pattern every other
     * event-driven write in this platform uses (see e.g. Pickup's own
     * createFromClaim). Idempotent on pickup_task_id (unique index, V1
     * migration) so a redelivered event never double-counts impact (§7).
     */
    public void recordFromPickupCompleted(PickupCompletedEvent event) {
        TenantContext.set(event.tenantId());
        try {
            if (rescueRecordRepository.existsByPickupTaskId(event.pickupTaskId())) {
                log.info("Rescue record already exists for pickupTaskId={}, skipping (idempotent redelivery)",
                        event.pickupTaskId());
                return;
            }
            FoodListingDto listing = foodServiceClient.getFoodListing(event.tenantId(), event.foodListingId());
            BigDecimal kgSaved = ImpactCalculator.estimateKgSaved(listing.quantityValue(), listing.quantityUnit());
            BigDecimal co2SavedKg = ImpactCalculator.estimateCo2SavedKg(kgSaved);

            RescueRecord record = new RescueRecord(
                    event.tenantId(), event.pickupTaskId(), event.foodListingId(), event.donorUserId(),
                    listing.donorOrgId(), event.receiverUserId(), listing.foodCategory(), listing.quantityValue(),
                    listing.quantityUnit(), kgSaved, co2SavedKg, event.occurredAt());
            rescueRecordRepository.save(record);
        } catch (RuntimeException e) {
            // No HTTP caller to return an error to; logged so a genuine
            // failure (e.g. Food unreachable) is visible rather than
            // silently dropping impact data. Kafka's redelivery means this
            // will be retried on the next delivery attempt.
            log.warn("Could not record impact for pickupTaskId={}: {}", event.pickupTaskId(), e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public ImpactSummary getDonorImpact(UUID userId) {
        return rescueRecordRepository.summarizeAsDonor(userId);
    }

    @Transactional(readOnly = true)
    public ImpactSummary getReceiverImpact(UUID userId) {
        return rescueRecordRepository.summarizeAsReceiver(userId);
    }

    @Transactional(readOnly = true)
    public ImpactSummary getOrgImpact(UUID orgId) {
        return rescueRecordRepository.summarizeByDonorOrg(orgId);
    }

    @Transactional(readOnly = true)
    public ImpactSummary getCommunityImpact() {
        return rescueRecordRepository.summarizeAll();
    }
}
