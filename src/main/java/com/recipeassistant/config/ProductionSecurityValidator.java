package com.recipeassistant.config;

import com.recipeassistant.security.SecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
public class ProductionSecurityValidator {

    @Value("${app.encryption.key:}")
    private String encryptionKey;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionSecrets() {
        if (!StringUtils.hasText(encryptionKey)
            || SecurityConstants.INSECURE_PLACEHOLDER_ENCRYPTION_KEY.equals(encryptionKey)) {
            throw new IllegalStateException(
                "APP_ENCRYPTION_KEY must be set to a unique secret in production (not the placeholder).");
        }
        if (!StringUtils.hasText(allowedOrigins)) {
            throw new IllegalStateException(
                "APP_CORS_ALLOWED_ORIGINS must be set in production (your public app URL).");
        }
    }
}
