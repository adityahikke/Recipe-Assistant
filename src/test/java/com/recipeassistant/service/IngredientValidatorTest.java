package com.recipeassistant.service;

import com.recipeassistant.model.IngredientValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IngredientValidatorTest {

    @Test
    void rejectsBlankWithoutCallingGemini() {
        IngredientValidator validator = new IngredientValidator(null);
        IngredientValidationResult result = validator.validate("AQ.test", "   ", null);
        assertFalse(result.valid());
        assertTrue(result.message().contains("at least one"));
    }

    @Test
    void failBuildsDefaultMessageForMultipleItems() {
        IngredientValidationResult result = IngredientValidationResult.fail(List.of("car", "temple"), null);
        assertFalse(result.valid());
        assertTrue(result.message().contains("car, temple"));
        assertTrue(result.message().toLowerCase().contains("food ingredient"));
    }

    @Test
    void failUsesLlmMessageWhenProvided() {
        String custom = "car and temple are not food ingredients.";
        IngredientValidationResult result = IngredientValidationResult.fail(List.of("car", "temple"), custom);
        assertEquals(custom, result.message());
    }
}
