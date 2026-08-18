package com.recipeassistant.model;

import jakarta.validation.constraints.NotBlank;

public class LibraryRecipeRequest {

    @NotBlank(message = "Recipe id is required")
    private String recipeId;

    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
}
