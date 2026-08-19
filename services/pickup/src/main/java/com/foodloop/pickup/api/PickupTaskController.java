package com.foodloop.pickup.api;

import com.foodloop.pickup.application.PickupService;
import com.foodloop.pickup.domain.PickupTask;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PickupTaskController {

    private final PickupService pickupService;

    public PickupTaskController(PickupService pickupService) {
        this.pickupService = pickupService;
    }

    @GetMapping("/api/v1/pickups/{id}")
    public PickupTaskResponse get(@PathVariable UUID id) {
        return PickupTaskResponse.from(pickupService.get(id));
    }

    @PostMapping("/api/v1/pickups/{id}/complete")
    public PickupTaskResponse complete(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        PickupTask task = pickupService.complete(id, callerUserId(authentication));
        return PickupTaskResponse.from(task);
    }

    @PostMapping("/api/v1/pickups/{id}/report-no-show")
    public PickupTaskResponse reportNoShow(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        PickupTask task = pickupService.reportNoShow(id, callerUserId(authentication));
        return PickupTaskResponse.from(task);
    }

    private UUID callerUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getToken().getSubject());
    }
}
