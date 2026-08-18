package com.recipeassistant.model;

import java.util.List;

public class RecipeResponse {
    private boolean success;
    private String content;
    private String error;
    private Boolean hasUserKey;
    private Boolean defaultKeyAvailable;
    private Integer defaultTrialsRemaining;
    private Integer defaultRecipesMax;
    private Integer defaultRecipesUsed;
    private String keySource;
    private String recipeId;
    private List<FavoriteRecipeDto> favorites;
    private StructuredRecipe recipe;
    private CookingTipsResult cookingTips;
    private Boolean favorited;
    private String trialDeviceNotice;
    private String contentHash;
    private List<CollectionDto> collections;
    private CollectionDto activeCollection;
    private List<RecipeCardDto> recipeCards;
    private Integer totalRecipes;
    private String savedIngredients;
    private String savedCuisine;
    private String savedDietaryRestrictions;
    private List<String> detectedIngredients;
    private String detectedSummary;

    public RecipeResponse() {}

    public RecipeResponse(boolean success, String content, String error) {
        this.success = success;
        this.content = content;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Boolean getHasUserKey() {
        return hasUserKey;
    }

    public void setHasUserKey(Boolean hasUserKey) {
        this.hasUserKey = hasUserKey;
    }

    public Boolean getDefaultKeyAvailable() {
        return defaultKeyAvailable;
    }

    public void setDefaultKeyAvailable(Boolean defaultKeyAvailable) {
        this.defaultKeyAvailable = defaultKeyAvailable;
    }

    public Integer getDefaultTrialsRemaining() {
        return defaultTrialsRemaining;
    }

    public void setDefaultTrialsRemaining(Integer defaultTrialsRemaining) {
        this.defaultTrialsRemaining = defaultTrialsRemaining;
    }

    public String getKeySource() {
        return keySource;
    }

    public void setKeySource(String keySource) {
        this.keySource = keySource;
    }

    public Integer getDefaultRecipesMax() {
        return defaultRecipesMax;
    }

    public void setDefaultRecipesMax(Integer defaultRecipesMax) {
        this.defaultRecipesMax = defaultRecipesMax;
    }

    public Integer getDefaultRecipesUsed() {
        return defaultRecipesUsed;
    }

    public void setDefaultRecipesUsed(Integer defaultRecipesUsed) {
        this.defaultRecipesUsed = defaultRecipesUsed;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public List<FavoriteRecipeDto> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<FavoriteRecipeDto> favorites) {
        this.favorites = favorites;
    }

    public StructuredRecipe getRecipe() {
        return recipe;
    }

    public void setRecipe(StructuredRecipe recipe) {
        this.recipe = recipe;
    }

    public CookingTipsResult getCookingTips() {
        return cookingTips;
    }

    public void setCookingTips(CookingTipsResult cookingTips) {
        this.cookingTips = cookingTips;
    }

    public Boolean getFavorited() {
        return favorited;
    }

    public void setFavorited(Boolean favorited) {
        this.favorited = favorited;
    }

    public String getTrialDeviceNotice() {
        return trialDeviceNotice;
    }

    public void setTrialDeviceNotice(String trialDeviceNotice) {
        this.trialDeviceNotice = trialDeviceNotice;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public List<CollectionDto> getCollections() {
        return collections;
    }

    public void setCollections(List<CollectionDto> collections) {
        this.collections = collections;
    }

    public CollectionDto getActiveCollection() {
        return activeCollection;
    }

    public void setActiveCollection(CollectionDto activeCollection) {
        this.activeCollection = activeCollection;
    }

    public List<RecipeCardDto> getRecipeCards() {
        return recipeCards;
    }

    public void setRecipeCards(List<RecipeCardDto> recipeCards) {
        this.recipeCards = recipeCards;
    }

    public Integer getTotalRecipes() {
        return totalRecipes;
    }

    public void setTotalRecipes(Integer totalRecipes) {
        this.totalRecipes = totalRecipes;
    }

    public String getSavedIngredients() {
        return savedIngredients;
    }

    public void setSavedIngredients(String savedIngredients) {
        this.savedIngredients = savedIngredients;
    }

    public String getSavedCuisine() {
        return savedCuisine;
    }

    public void setSavedCuisine(String savedCuisine) {
        this.savedCuisine = savedCuisine;
    }

    public String getSavedDietaryRestrictions() {
        return savedDietaryRestrictions;
    }

    public void setSavedDietaryRestrictions(String savedDietaryRestrictions) {
        this.savedDietaryRestrictions = savedDietaryRestrictions;
    }

    public List<String> getDetectedIngredients() {
        return detectedIngredients;
    }

    public void setDetectedIngredients(List<String> detectedIngredients) {
        this.detectedIngredients = detectedIngredients;
    }

    public String getDetectedSummary() {
        return detectedSummary;
    }

    public void setDetectedSummary(String detectedSummary) {
        this.detectedSummary = detectedSummary;
    }
}
