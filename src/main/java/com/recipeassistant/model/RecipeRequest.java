package com.recipeassistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecipeRequest {
    
    @NotBlank(message = "Ingredients are required")
    @Size(max = 1000, message = "Ingredients description too long (max 1000 characters)")
    private String ingredients;
    
    @Size(max = 100, message = "Cuisine name too long (max 100 characters)")
    private String cuisine;
    
    @Size(max = 500, message = "Dietary restrictions too long (max 500 characters)")
    private String dietaryRestrictions;

    private boolean onlyListedIngredients;

    public RecipeRequest() {}

    public RecipeRequest(String ingredients, String cuisine, String dietaryRestrictions) {
        this.ingredients = ingredients;
        this.cuisine = cuisine;
        this.dietaryRestrictions = dietaryRestrictions;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public String getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public void setDietaryRestrictions(String dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
    }

    public boolean isOnlyListedIngredients() {
        return onlyListedIngredients;
    }

    public void setOnlyListedIngredients(boolean onlyListedIngredients) {
        this.onlyListedIngredients = onlyListedIngredients;
    }
}
