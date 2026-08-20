package com.foodloop.ai.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.foodloop.ai.domain.AgentRunRepository;
import com.foodloop.ai.domain.PendingNgoAllocationRepository;
import com.foodloop.ai.tool.ToolExecutor;
import com.foodloop.ai.tool.matching.CreateMatchProposalTool;
import com.foodloop.commons.web.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Direct coverage of the realm-role check resolving an escalation requires (see the controller's Javadoc). */
class AgentRunControllerAuthorizationTest {

    private final AgentRunController controller = new AgentRunController(
            mock(AgentRunRepository.class), mock(PendingNgoAllocationRepository.class),
            mock(ToolExecutor.class), mock(CreateMatchProposalTool.class));

    @Test
    void allowsNgoOps() {
        assertThatCode(() -> controller.requireNgoOpsCaller(jwtWithRealmRoles(List.of("NGO_OPS"))))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsAdmin() {
        assertThatCode(() -> controller.requireNgoOpsCaller(jwtWithRealmRoles(List.of("ADMIN"))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsADonorWithNoPrivilegedRole() {
        assertThatThrownBy(() -> controller.requireNgoOpsCaller(jwtWithRealmRoles(List.of("DONOR"))))
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
        assertThatThrownBy(() -> controller.requireNgoOpsCaller(new JwtAuthenticationToken(jwt)))
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
