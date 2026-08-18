package com.recipeassistant.model;

public class FavoriteRecipeDto {
    private String id;
    private String title;
    private String content;
    private String ingredients;
    private String cuisine;
    private String dietaryRestrictions;
    private String createdAt;
    private String contentHash;
    private StructuredRecipe structuredRecipe;

    public FavoriteRecipeDto() {}

    public FavoriteRecipeDto(String id, String title, String content, String ingredients,
                             String cuisine, String dietaryRestrictions, String createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.ingredients = ingredients;
        this.cuisine = cuisine;
        this.dietaryRestrictions = dietaryRestrictions;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public String getDietaryRestrictions() { return dietaryRestrictions; }
    public void setDietaryRestrictions(String dietaryRestrictions) { this.dietaryRestrictions = dietaryRestrictions; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public StructuredRecipe getStructuredRecipe() { return structuredRecipe; }
    public void setStructuredRecipe(StructuredRecipe structuredRecipe) { this.structuredRecipe = structuredRecipe; }
}
