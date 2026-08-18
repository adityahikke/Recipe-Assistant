package com.recipeassistant.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StructuredRecipe {
    private String name;
    private String preparationTime;
    private String cookingTime;
    private String servings;
    private List<String> ingredients = new ArrayList<>();
    private List<String> instructions = new ArrayList<>();
    private List<String> tips = new ArrayList<>();
    private Map<String, String> nutrition = new LinkedHashMap<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPreparationTime() { return preparationTime; }
    public void setPreparationTime(String preparationTime) { this.preparationTime = preparationTime; }

    public String getCookingTime() { return cookingTime; }
    public void setCookingTime(String cookingTime) { this.cookingTime = cookingTime; }

    public String getServings() { return servings; }
    public void setServings(String servings) { this.servings = servings; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public List<String> getInstructions() { return instructions; }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }

    public List<String> getTips() { return tips; }
    public void setTips(List<String> tips) { this.tips = tips; }

    public Map<String, String> getNutrition() { return nutrition; }
    public void setNutrition(Map<String, String> nutrition) { this.nutrition = nutrition; }
}
