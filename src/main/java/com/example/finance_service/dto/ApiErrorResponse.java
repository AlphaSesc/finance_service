package com.example.finance_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
// Standardized API error response used across all microservices
// Ensures consistent error structure for client applications
public class ApiErrorResponse {
    // Time when the error occurred
    private LocalDateTime timestamp;

    // HTTP status code (e.g., 400, 404, 409)
    private int status;

    // Short error type (e.g., Bad Request, Not Found)
    private String error;

    // Detailed error message
    private String message;

    // API endpoint path where the error occurred
    private String path;

    // Field-level validation errors (used for request validation failures)
    private Map<String, String> validationErrors;
}