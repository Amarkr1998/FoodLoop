package com.foodloop.food.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.foodloop.commons.web.ApiException;
import com.foodloop.food.application.ClaimService;
import com.foodloop.food.application.FoodListingService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Direct coverage of the one security-critical check this endpoint adds
 * (see the controller's Javadoc): a caller's own signed {@code azp} claim,
 * not anything else in the request, decides whether AI-metadata writes are
 * permitted.
 */
class FoodListingControllerAiMetadataAuthorizationTest {

    private final FoodListingController controller =
            new FoodListingController(mock(FoodListingService.class), mock(ClaimService.class));

    @Test
    void allowsTheAiOrchestrationServiceAccount() {
        assertThatCode(() -> controller.requireAiOrchestrationCaller(jwtWithAzp("foodloop-ai-orchestration")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsEveryOtherCaller() {
        assertThatThrownBy(() -> controller.requireAiOrchestrationCaller(jwtWithAzp("foodloop-web")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiException = (ApiException) e;
                    org.assertj.core.api.Assertions.assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void rejectsATokenWithNoAzpClaimAtAll() {
        assertThatThrownBy(() -> controller.requireAiOrchestrationCaller(jwtWithAzp(null)))
                .isInstanceOf(ApiException.class);
    }

    private JwtAuthenticationToken jwtWithAzp(String azp) {
        var claims = new java.util.HashMap<String, Object>();
        if (azp != null) {
            claims.put("azp", azp);
        }
        claims.put("sub", UUID.randomUUID().toString());
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
