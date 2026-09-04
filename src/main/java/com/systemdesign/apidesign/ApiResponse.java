package com.systemdesign.apidesign;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard API Response Envelope Pattern (Category 10).
 * <p>
 * Ensures all API endpoints return a uniform, self-describing payload format
 * containing data, error details, correlation metadata, and timestamps.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        String correlationId,
        Instant timestamp
) {
    public record ApiError(String code, String message) {}

    public static <T> ApiResponse<T> ok(T data, String correlationId) {
        return new ApiResponse<>(true, data, null, correlationId, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message, String correlationId) {
        return new ApiResponse<>(false, null, new ApiError(code, message), correlationId, Instant.now());
    }
}
