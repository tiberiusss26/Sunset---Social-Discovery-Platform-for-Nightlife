package com.nightout.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
) {
    public static ApiError of(org.springframework.http.HttpStatus status, String message) {
        return new ApiError(status.value(), status.getReasonPhrase(),
                message, LocalDateTime.now(), null);
    }

    public static ApiError withFields(org.springframework.http.HttpStatus status,
                                      String message,
                                      Map<String, String> fields) {
        return new ApiError(status.value(), status.getReasonPhrase(),
                message, LocalDateTime.now(), fields);
    }
}