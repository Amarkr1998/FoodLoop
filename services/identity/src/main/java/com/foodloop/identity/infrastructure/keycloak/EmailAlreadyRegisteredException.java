package com.foodloop.identity.infrastructure.keycloak;

import com.foodloop.commons.web.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApiException {

    public EmailAlreadyRegisteredException(String email) {
        super("USER_ALREADY_EXISTS", HttpStatus.CONFLICT, "An account with email " + email + " already exists.");
    }
}
