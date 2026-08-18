package com.recipeassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipeassistant.model.CollectionDto;
import com.recipeassistant.model.Recipe;
import com.recipeassistant.model.RecipeCollection;
import com.recipeassistant.model.StructuredRecipe;
import com.recipeassistant.service.RecipeLibraryService;
import com.recipeassistant.util.ContentHashUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class RecipeLibraryServiceTest extends AbstractIntegrationTest {

    @Autowired private RecipeLibraryService libraryService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void persistGeneratedRecipe_autoAddsToMyRecipes_andToggleSaved() throws Exception {
        String clientId = UUID.randomUUID().toString();
        StructuredRecipe structured = sampleRecipe("Cleanup Sprint Pasta");
        String recipeJson = objectMapper.writeValueAsString(structured);
        String contentHash = ContentHashUtil.sha256(recipeJson);

        Recipe saved = libraryService.persistGeneratedRecipe(
            clientId, structured, recipeJson, contentHash,
            "pasta, garlic", "Italian", "vegetarian"
        );

        assertNotNull(saved.getId());
        assertEquals(1, libraryService.countRecipes(clientId));
        assertFalse(libraryService.isInSavedCollection(clientId, saved));

        List<CollectionDto> collections = libraryService.listCollections(clientId);
        CollectionDto myRecipes = collections.stream()
            .filter(c -> RecipeCollection.SLUG_MY_RECIPES.equals(c.getSlug()))
            .findFirst()
            .orElseThrow();
        CollectionDto savedCol = collections.stream()
            .filter(c -> RecipeCollection.SLUG_SAVED.equals(c.getSlug()))
            .findFirst()
            .orElseThrow();

        assertEquals(1, myRecipes.getRecipeCount());
        assertEquals(0, savedCol.getRecipeCount());

        RecipeLibraryService.SavedToggleResult toggledOn =
            libraryService.toggleSaved(clientId, saved.getId());
        assertTrue(toggledOn.saved());
        assertTrue(libraryService.isInSavedCollection(clientId, saved));

        collections = libraryService.listCollections(clientId);
        savedCol = collections.stream()
            .filter(c -> RecipeCollection.SLUG_SAVED.equals(c.getSlug()))
            .findFirst()
            .orElseThrow();
        assertEquals(1, savedCol.getRecipeCount());

        RecipeLibraryService.SavedToggleResult toggledOff =
            libraryService.toggleSaved(clientId, saved.getId());
        assertFalse(toggledOff.saved());
        assertFalse(libraryService.isInSavedCollection(clientId, saved));
    }

    @Test
    void persistGeneratedRecipe_dedupesByContentHash() throws Exception {
        String clientId = UUID.randomUUID().toString();
        StructuredRecipe structured = sampleRecipe("Deduped Bowl");
        String recipeJson = objectMapper.writeValueAsString(structured);
        String contentHash = ContentHashUtil.sha256(recipeJson);

        Recipe first = libraryService.persistGeneratedRecipe(
            clientId, structured, recipeJson, contentHash, "rice", "", "");
        Recipe second = libraryService.persistGeneratedRecipe(
            clientId, structured, recipeJson, contentHash, "rice", "", "");

        assertEquals(first.getId(), second.getId());
        assertEquals(1, libraryService.countRecipes(clientId));
    }

    private static StructuredRecipe sampleRecipe(String name) {
        StructuredRecipe recipe = new StructuredRecipe();
        recipe.setName(name);
        recipe.setPreparationTime("10 min");
        recipe.setCookingTime("20 min");
        recipe.setServings("2");
        recipe.setIngredients(List.of("pasta", "garlic"));
        recipe.setInstructions(List.of("Boil pasta.", "Serve."));
        recipe.setTips(List.of("Salt the water."));
        recipe.setNutrition(Map.of(
            "calories", "400",
            "protein", "12g",
            "carbs", "60g",
            "fat", "8g"
        ));
        return recipe;
    }
}
