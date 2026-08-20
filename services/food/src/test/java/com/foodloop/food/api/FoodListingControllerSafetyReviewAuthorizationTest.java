package com.foodloop.food.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.foodloop.commons.web.ApiException;
import com.foodloop.food.application.ClaimService;
import com.foodloop.food.application.FoodListingService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Direct coverage of the realm-role check clearing a safety hold requires (see the controller's Javadoc). */
class FoodListingControllerSafetyReviewAuthorizationTest {

    private final FoodListingController controller =
            new FoodListingController(mock(FoodListingService.class), mock(ClaimService.class));

    @Test
    void allowsTrustOps() {
        assertThatCode(() -> controller.requireTrustOpsCaller(jwtWithRealmRoles(List.of("TRUST_OPS"))))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsAdmin() {
        assertThatCode(() -> controller.requireTrustOpsCaller(jwtWithRealmRoles(List.of("ADMIN"))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsADonorWithNoPrivilegedRole() {
        assertThatThrownBy(() -> controller.requireTrustOpsCaller(jwtWithRealmRoles(List.of("DONOR"))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsATokenWithNoRealmAccessClaimAtAll() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        assertThatThrownBy(() -> controller.requireTrustOpsCaller(new JwtAuthenticationToken(jwt)))
                .isInstanceOf(ApiException.class);
    }

    private JwtAuthenticationToken jwtWithRealmRoles(List<String> roles) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
