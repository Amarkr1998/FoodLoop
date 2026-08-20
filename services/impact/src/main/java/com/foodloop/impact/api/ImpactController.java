package com.foodloop.impact.api;

import com.foodloop.impact.application.ImpactService;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ImpactController {

    private final ImpactService impactService;

    public ImpactController(ImpactService impactService) {
        this.impactService = impactService;
    }

    @GetMapping("/api/v1/impact/me")
    public PersonalImpactResponse getMyImpact(JwtAuthenticationToken auth) {
        UUID userId = callerUserId(auth);
        return PersonalImpactResponse.of(
                impactService.getDonorImpact(userId), impactService.getReceiverImpact(userId));
    }

    @GetMapping("/api/v1/impact/organizations/{orgId}")
    public ImpactSummaryResponse getOrganizationImpact(@PathVariable UUID orgId) {
        return ImpactSummaryResponse.from(impactService.getOrgImpact(orgId));
    }

    @GetMapping("/api/v1/impact/community")
    public ImpactSummaryResponse getCommunityImpact() {
        return ImpactSummaryResponse.from(impactService.getCommunityImpact());
    }

    private UUID callerUserId(JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();
        return UUID.fromString(jwt.getSubject());
    }
}
