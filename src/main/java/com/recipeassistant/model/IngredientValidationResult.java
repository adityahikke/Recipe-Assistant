package com.recipeassistant.model;

import java.util.List;

public record IngredientValidationResult(boolean valid, List<String> unrecognized, String message) {

    public static IngredientValidationResult ok() {
        return new IngredientValidationResult(true, List.of(), null);
    }

    public static IngredientValidationResult fail(List<String> unrecognized, String message) {
        String msg = (message != null && !message.isBlank())
            ? message.trim()
            : buildDefaultMessage(unrecognized);
        return new IngredientValidationResult(false, List.copyOf(unrecognized), msg);
    }

    private static String buildDefaultMessage(List<String> unrecognized) {
        if (unrecognized.isEmpty()) {
            return "Please enter real food ingredients (e.g. chicken, rice, tomatoes).";
        }
        String joined = String.join(", ", unrecognized);
        return unrecognized.size() == 1
            ? "\"" + joined + "\" doesn't look like a food ingredient. Try items like chicken, rice, or tomatoes."
            : "These don't look like food ingredients: " + joined
                + ". Please enter real pantry items (e.g. chicken, rice, tomatoes).";
    }
}
