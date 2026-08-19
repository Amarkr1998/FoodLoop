package com.foodloop.identity.infrastructure.keycloak;

import com.foodloop.commons.web.ApiException;
import org.springframework.http.HttpStatus;

public class KeycloakProvisioningException extends ApiException {

    public KeycloakProvisioningException(String message) {
        super("IDENTITY_PROVIDER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
