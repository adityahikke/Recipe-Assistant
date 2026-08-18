package com.recipeassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    public void logApiKeySet(String clientIp) {
        logSecurityEvent("API_KEY_SET", clientIp, "API key successfully set");
    }

    public void logApiKeyValidationFailed(String clientIp, String reason) {
        logSecurityEvent("API_KEY_VALIDATION_FAILED", clientIp, "API key validation failed: " + reason);
    }

    public void logInvalidInput(String clientIp, String endpoint, String details) {
        logSecurityEvent("INVALID_INPUT", clientIp, "Invalid input at " + endpoint + ": " + details);
    }

    public void logRecipeGeneration(String clientIp, String ingredients) {
        logSecurityEvent("RECIPE_GENERATION", clientIp, "Recipe generated for ingredients: " + sanitizeInput(ingredients));
    }

    private void logSecurityEvent(String eventType, String clientIp, String details) {
        log.info("[SECURITY] {} | {} | {}", eventType, clientIp, details);
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "null";
        }
        return input.length() > 100 ? input.substring(0, 100) + "..." : input;
    }
}
