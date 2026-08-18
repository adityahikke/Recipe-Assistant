package com.recipeassistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ApiKeyRequest {

    @NotBlank(message = "API key is required")
    @Size(min = 20, max = 256, message = "API key length is invalid")
    @Pattern(regexp = "^sk-[A-Za-z0-9_-]+$", message = "Invalid API key format")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
