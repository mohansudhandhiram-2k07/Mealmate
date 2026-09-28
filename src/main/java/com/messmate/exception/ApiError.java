package com.messmate.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/** The one JSON shape every error response uses. No stack traces, ever. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(LocalDateTime timestamp, int status, String error, String message,
                       Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
