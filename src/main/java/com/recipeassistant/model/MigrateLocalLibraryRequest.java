package com.recipeassistant.model;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class MigrateLocalLibraryRequest {

    @Size(max = 50, message = "Too many recipes to import at once (max 50).")
    private List<FavoriteRecipeDto> favorites = new ArrayList<>();

    public List<FavoriteRecipeDto> getFavorites() { return favorites; }
    public void setFavorites(List<FavoriteRecipeDto> favorites) { this.favorites = favorites; }
}
