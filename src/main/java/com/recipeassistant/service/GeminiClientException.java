package com.recipeassistant.service;

import org.springframework.http.HttpStatusCode;

public class GeminiClientException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String errorDetails;

    public GeminiClientException(HttpStatusCode statusCode) {
        this(statusCode, null);
    }

    public GeminiClientException(String message) {
        super(message);
        this.statusCode = HttpStatusCode.valueOf(500);
        this.errorDetails = message;
    }

    public GeminiClientException(HttpStatusCode statusCode, String errorDetails) {
        super("Gemini API error: " + statusCode + (errorDetails != null ? " - " + errorDetails : ""));
        this.statusCode = statusCode;
        this.errorDetails = errorDetails;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public boolean isUnauthorizedOrForbidden() {
        if (statusCode != null) {
            int val = statusCode.value();
            return val == 401 || val == 403;
        }
        return false;
    }

    public boolean isBadRequest() {
        return statusCode != null && statusCode.value() == 400;
    }

    public boolean isRateLimitedOrQuotaExceeded() {
        return statusCode != null && statusCode.value() == 429;
    }

    public boolean isServiceUnavailable() {
        return statusCode != null && statusCode.value() == 503;
    }
}
