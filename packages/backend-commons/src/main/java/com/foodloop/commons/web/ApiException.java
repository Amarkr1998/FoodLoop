package com.foodloop.commons.web;

import org.springframework.http.HttpStatus;

/**
 * Base type for domain-facing errors a controller wants surfaced to the
 * client as a well-formed {@link ApiError} (e.g. {@code FOOD_ALREADY_CLAIMED}),
 * as opposed to an unexpected exception that should be logged and hidden
 * behind a generic 500.
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
