package com.recipeassistant.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiKeyService {

    public static final String SESSION_USER_KEY = "encrypted_gemini_api_key";

    @Value("${gemini.api.default-key:}")
    private String defaultApiKey;

    @Value("${app.default-key.max-recipes-per-session:5}")
    private int maxDefaultRecipesPerSession;

    @Autowired
    private TrialClientService trialClientService;

    public boolean isDefaultKeyConfigured() {
        return defaultApiKey != null && !defaultApiKey.isBlank();
    }

    public boolean hasUserKey(HttpSession session) {
        return session.getAttribute(SESSION_USER_KEY) != null;
    }

    public int getMaxDefaultRecipesPerSession() {
        return maxDefaultRecipesPerSession;
    }

    public int getDefaultRecipeCount(String clientId) {
        return trialClientService.getRecipeCount(clientId);
    }

    public int getDefaultTrialsRemaining(String clientId) {
        if (!isDefaultKeyConfigured()) {
            return 0;
        }
        return Math.max(0, maxDefaultRecipesPerSession - getDefaultRecipeCount(clientId));
    }

    public int getDefaultRecipesUsed(String clientId) {
        return getDefaultRecipeCount(clientId);
    }

    public void incrementDefaultRecipeCount(String clientId, HttpServletRequest request) {
        trialClientService.incrementRecipeCount(clientId, request);
    }

    public ResolvedKey resolveKey(HttpSession session, String clientId, EncryptionService encryptionService,
                                  boolean countAgainstTrial) {
        String encryptedUserKey = (String) session.getAttribute(SESSION_USER_KEY);
        if (encryptedUserKey != null) {
            String userKey = encryptionService.decrypt(encryptedUserKey);
            if (userKey != null) {
                return ResolvedKey.ok(userKey, KeySource.USER, false);
            }
            return ResolvedKey.error("Error decrypting API key. Please set it again.");
        }

        if (!isDefaultKeyConfigured()) {
            return ResolvedKey.error("Please set your Gemini API key first.");
        }

        if (getDefaultRecipeCount(clientId) >= maxDefaultRecipesPerSession) {
            return ResolvedKey.error(
                "Free trial limit reached (" + maxDefaultRecipesPerSession
                    + " recipes on this device). Add your own Gemini API key to continue.");
        }

        return ResolvedKey.ok(defaultApiKey.trim(), KeySource.DEFAULT, countAgainstTrial);
    }

    public enum KeySource {
        USER,
        DEFAULT
    }

    public static final class ResolvedKey {
        private final boolean valid;
        private final String apiKey;
        private final KeySource source;
        private final boolean incrementTrialOnSuccess;
        private final String errorMessage;

        private ResolvedKey(boolean valid, String apiKey, KeySource source, boolean incrementTrialOnSuccess, String errorMessage) {
            this.valid = valid;
            this.apiKey = apiKey;
            this.source = source;
            this.incrementTrialOnSuccess = incrementTrialOnSuccess;
            this.errorMessage = errorMessage;
        }

        public static ResolvedKey ok(String apiKey, KeySource source, boolean incrementTrialOnSuccess) {
            return new ResolvedKey(true, apiKey, source, incrementTrialOnSuccess, null);
        }

        public static ResolvedKey error(String errorMessage) {
            return new ResolvedKey(false, null, null, false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getApiKey() {
            return apiKey;
        }

        public KeySource getSource() {
            return source;
        }

        public boolean isIncrementTrialOnSuccess() {
            return incrementTrialOnSuccess;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
