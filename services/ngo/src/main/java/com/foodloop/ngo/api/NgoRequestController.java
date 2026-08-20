package com.foodloop.ngo.api;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.ngo.application.NgoRequestService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NgoRequestController {

    private final NgoRequestService ngoRequestService;

    public NgoRequestController(NgoRequestService ngoRequestService) {
        this.ngoRequestService = ngoRequestService;
    }

    @PostMapping("/api/v1/ngo/requests")
    public ResponseEntity<NgoRequestResponse> create(@Valid @RequestBody CreateNgoRequestRequest request) {
        var ngoRequest = ngoRequestService.create(
                TenantContext.get(), request.ngoOrgId(), request.foodCategory(), request.quantityNeeded(),
                request.quantityUnit(), request.neededBefore(), request.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(NgoRequestResponse.from(ngoRequest));
    }

    @GetMapping("/api/v1/ngo/requests/{id}")
    public NgoRequestResponse get(@PathVariable UUID id) {
        return NgoRequestResponse.from(ngoRequestService.get(id));
    }

    @GetMapping("/api/v1/ngo/requests")
    public List<NgoRequestResponse> listForOrg(@RequestParam UUID ngoOrgId) {
        return ngoRequestService.listForOrg(ngoOrgId).stream().map(NgoRequestResponse::from).toList();
    }

    /** The NGO Coordination Agent's scheduled sweep reads this (spec §19) — optionally bounded to requests nearing their deadline. */
    @GetMapping("/api/v1/ngo/requests/open")
    public List<NgoRequestResponse> listOpen(@RequestParam(required = false) Instant neededBeforeOrAt) {
        var open = neededBeforeOrAt != null
                ? ngoRequestService.listOpenNearingDeadline(neededBeforeOrAt)
                : ngoRequestService.listOpen();
        return open.stream().map(NgoRequestResponse::from).toList();
    }

    @PostMapping("/api/v1/ngo/requests/{id}/cancel")
    public NgoRequestResponse cancel(@PathVariable UUID id) {
        return NgoRequestResponse.from(ngoRequestService.cancel(id));
    }

    @PostMapping("/api/v1/ngo/requests/{id}/fulfilled")
    public NgoRequestResponse markFulfilled(@PathVariable UUID id) {
        return NgoRequestResponse.from(ngoRequestService.markFulfilled(id));
    }
}
