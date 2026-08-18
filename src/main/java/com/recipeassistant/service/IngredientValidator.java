package com.recipeassistant.service;

import com.recipeassistant.model.IngredientValidationResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngredientValidator {

    private final GeminiService geminiService;

    public IngredientValidator(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public IngredientValidationResult validate(String apiKey, String raw, Runnable cancellationCheck) {
        if (raw == null || raw.isBlank()) {
            return IngredientValidationResult.fail(List.of(), "Please enter at least one food ingredient.");
        }
        return geminiService.validateIngredients(apiKey, raw.trim(), cancellationCheck);
    }
}
