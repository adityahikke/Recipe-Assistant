package com.recipeassistant.model;

public class CollectionDto {
    private String id;
    private String title;
    private String description;
    private String slug;
    private boolean systemDefault;
    private int recipeCount;
    private int sortOrder;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public boolean isSystemDefault() { return systemDefault; }
    public void setSystemDefault(boolean systemDefault) { this.systemDefault = systemDefault; }

    public int getRecipeCount() { return recipeCount; }
    public void setRecipeCount(int recipeCount) { this.recipeCount = recipeCount; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
