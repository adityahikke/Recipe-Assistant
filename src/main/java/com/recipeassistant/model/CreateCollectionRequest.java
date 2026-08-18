package com.recipeassistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCollectionRequest {

    @NotBlank(message = "Collection title is required")
    @Size(max = 120, message = "Title is too long")
    private String title;

    @Size(max = 500, message = "Description is too long")
    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
