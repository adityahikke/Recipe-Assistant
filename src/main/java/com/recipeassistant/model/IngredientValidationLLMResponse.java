package com.recipeassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngredientValidationLLMResponse(
    boolean valid,
    List<String> invalidItems,
    String message
) {}
