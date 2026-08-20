package com.foodloop.pickup.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.foodloop.commons.web.ApiException;
import com.foodloop.pickup.application.PickupService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Direct coverage of the azp check the Pickup Agent's system-unassign/delayed-sweep endpoints require (see the controller's Javadoc). */
class PickupTaskControllerAuthorizationTest {

    private final PickupTaskController controller = new PickupTaskController(mock(PickupService.class));

    @Test
    void allowsAiOrchestrationServiceAccount() {
        assertThatCode(() -> controller.requireAiOrchestrationCaller(jwtWithAzp("foodloop-ai-orchestration")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnyOtherCaller() {
        assertThatThrownBy(() -> controller.requireAiOrchestrationCaller(jwtWithAzp("foodloop-web")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsATokenWithNoAzpClaimAtAll() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        assertThatThrownBy(() -> controller.requireAiOrchestrationCaller(new JwtAuthenticationToken(jwt)))
                .isInstanceOf(ApiException.class);
    }

    private JwtAuthenticationToken jwtWithAzp(String azp) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .claim("azp", azp)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
