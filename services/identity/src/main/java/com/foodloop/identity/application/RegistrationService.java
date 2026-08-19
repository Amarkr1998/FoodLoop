package com.foodloop.identity.application;

import com.foodloop.commons.tenant.TenantContext;
import com.foodloop.identity.api.RegisterUserRequest;
import com.foodloop.identity.domain.AppUser;
import com.foodloop.identity.domain.AppUserRepository;
import com.foodloop.identity.infrastructure.events.UserEventPublisher;
import com.foodloop.identity.infrastructure.events.UserRegisteredEvent;
import com.foodloop.identity.infrastructure.keycloak.KeycloakUserProvisioner;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final KeycloakUserProvisioner keycloakUserProvisioner;
    private final AppUserRepository appUserRepository;
    private final UserEventPublisher eventPublisher;

    public RegistrationService(
            KeycloakUserProvisioner keycloakUserProvisioner,
            AppUserRepository appUserRepository,
            UserEventPublisher eventPublisher) {
        this.keycloakUserProvisioner = keycloakUserProvisioner;
        this.appUserRepository = appUserRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * There is no authenticated caller yet during registration, so
     * {@link TenantContext} carries nothing from a JWT the way it does on
     * every other request. Setting it explicitly here to the tenant the new
     * row itself declares is the one legitimate exception to "tenant comes
     * from the token" (ADR-009): it is not a bypass of another tenant's
     * data, it is establishing which tenant this brand-new row belongs to.
     *
     * <p>Deliberately not {@code @Transactional} at this method's level:
     * {@link TenantContext} must be set <em>before</em> the JPA connection
     * is checked out (that's when {@code TenantAwareDataSource} stamps the
     * RLS GUC), and a Spring transaction proxy would begin the transaction
     * — and check out the connection — at method entry, before this body
     * runs. Relying on {@link AppUserRepository#save} to open its own
     * (correctly ordered) transaction keeps that sequencing intact.
     */
    public AppUser register(RegisterUserRequest request) {
        UUID keycloakId = keycloakUserProvisioner.createUser(
                request.tenantId(), request.email(), request.password(), request.displayName());

        TenantContext.set(request.tenantId());
        try {
            AppUser user = new AppUser(
                    keycloakId, request.tenantId(), request.email(), request.displayName(), request.locale());
            AppUser saved = appUserRepository.save(user);

            eventPublisher.publishUserRegistered(UserRegisteredEvent.of(
                    saved.getTenantId(), saved.getId(), saved.getEmail(), saved.getDisplayName(),
                    MDC.get("correlationId")));

            return saved;
        } finally {
            TenantContext.clear();
        }
    }
}
