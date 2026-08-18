package com.recipeassistant.model;

import java.util.List;

public record DetectedIngredientsResult(List<String> ingredients, String summary) {}
