package com.messmate.exception;

import org.springframework.http.HttpStatus;

/** Thrown by the service layer when a request is valid JSON but breaks a business rule. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException notFound(String what, Long id) {
        return new ApiException(HttpStatus.NOT_FOUND, what + " not found with id " + id);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
