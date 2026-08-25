package com.foodloop.identity.infrastructure.keycloak;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

/**
 * Creates the Keycloak account backing a new platform user. {@code tenant_id}
 * is stamped as a Keycloak user attribute at creation so it flows into every
 * future access token via each client's {@code tenant_id} protocol mapper
 * (infrastructure/docker/keycloak/foodloop-realm.json), which is what
 * {@code TenantFilter} (backend-commons) reads on every authenticated
 * request.
 */
@Component
public class KeycloakUserProvisioner {

    private final Keycloak keycloakAdminClient;
    private final KeycloakAdminProperties properties;

    public KeycloakUserProvisioner(Keycloak keycloakAdminClient, KeycloakAdminProperties properties) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.properties = properties;
    }

    public UUID createUser(UUID tenantId, String email, String password, String displayName) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setEmailVerified(false);
        user.setEnabled(true);
        user.setFirstName(displayName);
        user.setAttributes(Map.of("tenant_id", List.of(tenantId.toString())));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        try (Response response = keycloakAdminClient.realm(properties.realm()).users().create(user)) {
            if (response.getStatus() == 201) {
                return extractUserId(response);
            }
            if (response.getStatus() == 409) {
                throw new EmailAlreadyRegisteredException(email);
            }
            throw new KeycloakProvisioningException(
                    "Keycloak user creation failed with status " + response.getStatus());
        }
    }

    /**
     * Compensating action for when Keycloak account creation succeeds but the
     * platform-side profile write fails afterward (e.g. a race lets two
     * concurrent registrations both pass the pre-check) — best-effort only,
     * since the caller is already in an error path and must not let a
     * secondary failure here mask the original one.
     */
    public void deleteUser(UUID id) {
        keycloakAdminClient.realm(properties.realm()).users().get(id.toString()).remove();
    }

    /**
     * Phase 10's volunteer onboarding is this method's first caller — no
     * user has ever been granted a realm role by this platform before now
     * (every prior authorization check reads whatever roles a token already
     * carries, never assigns one). A role granted here only appears in a
     * <em>new</em> access token — Keycloak doesn't retroactively update a
     * token already issued — so a caller must re-authenticate to actually
     * use it; that's standard OIDC behavior, not a bug to work around.
     */
    public void assignRealmRole(UUID userId, String roleName) {
        RoleRepresentation role = keycloakAdminClient.realm(properties.realm()).roles().get(roleName).toRepresentation();
        keycloakAdminClient.realm(properties.realm()).users().get(userId.toString()).roles().realmLevel().add(List.of(role));
    }

    private UUID extractUserId(Response response) {
        String location = response.getHeaderString("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);
        return UUID.fromString(id);
    }
}
