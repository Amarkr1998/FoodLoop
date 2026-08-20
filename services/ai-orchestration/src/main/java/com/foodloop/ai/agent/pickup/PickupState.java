package com.foodloop.ai.agent.pickup;

import com.foodloop.ai.client.PickupTaskDto;
import com.foodloop.ai.client.VolunteerProfileDto;
import java.util.List;
import java.util.UUID;

record PickupState(
        UUID pickupTaskId,
        PickupTaskDto task,
        boolean noLongerNeedsAttention,
        List<VolunteerProfileDto> candidates,
        int notifiedCount,
        boolean reassigned) {

    static PickupState initial(UUID pickupTaskId) {
        return new PickupState(pickupTaskId, null, false, null, 0, false);
    }

    PickupState withTask(PickupTaskDto task) {
        boolean stillDelayable = task != null && task.assignedVolunteerId() != null
                && ("ASSIGNED".equals(task.status()) || "EN_ROUTE".equals(task.status()));
        return new PickupState(pickupTaskId, task, !stillDelayable, candidates, notifiedCount, reassigned);
    }

    PickupState withCandidates(List<VolunteerProfileDto> candidates) {
        return new PickupState(pickupTaskId, task, noLongerNeedsAttention, candidates, notifiedCount, reassigned);
    }

    PickupState withNotifiedCount(int notifiedCount) {
        return new PickupState(pickupTaskId, task, noLongerNeedsAttention, candidates, notifiedCount, reassigned);
    }

    PickupState withReassigned() {
        return new PickupState(pickupTaskId, task, noLongerNeedsAttention, candidates, notifiedCount, true);
    }
}
