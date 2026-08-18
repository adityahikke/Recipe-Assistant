package com.recipeassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipeassistant.model.*;
import com.recipeassistant.repository.CollectionRecipeRepository;
import com.recipeassistant.repository.RecipeCollectionRepository;
import com.recipeassistant.repository.RecipeRepository;
import com.recipeassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class RecipeLibraryService {

    private static final Pattern SLUG_SAFE = Pattern.compile("[^a-z0-9]+");

    @Autowired private UserRepository userRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private RecipeCollectionRepository collectionRepository;
    @Autowired private CollectionRecipeRepository collectionRecipeRepository;
    @Autowired private ObjectMapper objectMapper;

    public User resolveDeviceUser(String clientId) {
        return userRepository.findByClientId(clientId).orElseGet(() -> {
            User user = new User("device-" + clientId + "@recipeassistant.local");
            user.setClientId(clientId);
            user.setLastLogin(LocalDateTime.now());
            user = userRepository.save(user);
            ensureSystemCollections(user);
            return user;
        });
    }

    public Recipe persistGeneratedRecipe(String clientId, StructuredRecipe structured, String recipeJson,
                                         String contentHash, String ingredients, String cuisine,
                                         String dietaryRestrictions) {
        User user = resolveDeviceUser(clientId);
        ensureSystemCollections(user);

        Recipe recipe = recipeRepository.findByUserAndContentHash(user, contentHash).orElseGet(() -> {
            Recipe created = new Recipe(
                structured.getName() != null ? structured.getName() : "Untitled Recipe",
                recipeJson,
                user
            );
            created.setIngredients(ingredients);
            created.setCuisine(cuisine);
            created.setDietaryRestrictions(dietaryRestrictions);
            created.setContentHash(contentHash);
            return recipeRepository.save(created);
        });

        RecipeCollection myRecipes = getSystemCollection(user, RecipeCollection.SLUG_MY_RECIPES);
        addRecipeToCollection(myRecipes, recipe);
        return recipe;
    }

    public boolean isInSavedCollection(String clientId, Recipe recipe) {
        User user = resolveDeviceUser(clientId);
        RecipeCollection saved = getSystemCollection(user, RecipeCollection.SLUG_SAVED);
        return collectionRecipeRepository.existsByCollectionAndRecipe(saved, recipe);
    }

    public SavedToggleResult toggleSaved(String clientId, UUID recipeId) {
        User user = resolveDeviceUser(clientId);
        Recipe recipe = recipeRepository.findById(recipeId)
            .filter(r -> r.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found in your library."));

        RecipeCollection saved = getSystemCollection(user, RecipeCollection.SLUG_SAVED);
        if (collectionRecipeRepository.existsByCollectionAndRecipe(saved, recipe)) {
            collectionRecipeRepository.deleteByCollectionAndRecipe(saved, recipe);
            return new SavedToggleResult(recipe, false);
        }
        addRecipeToCollection(saved, recipe);
        return new SavedToggleResult(recipe, true);
    }

    public List<CollectionDto> listCollections(String clientId) {
        User user = resolveDeviceUser(clientId);
        ensureSystemCollections(user);
        Map<UUID, Integer> counts = recipeCountsByCollectionId(user);
        return collectionRepository.findByUserOrderBySortOrderAscCreatedAtAsc(user).stream()
            .map(c -> toCollectionDto(c, counts.getOrDefault(c.getId(), 0)))
            .toList();
    }

    public List<RecipeCardDto> listCollectionRecipes(String clientId, UUID collectionId) {
        User user = resolveDeviceUser(clientId);
        RecipeCollection collection = getOwnedCollection(user, collectionId);
        RecipeCollection saved = getSystemCollection(user, RecipeCollection.SLUG_SAVED);
        Set<UUID> savedIds = collectionRecipeRepository.findRecipeIdsByCollection(saved);
        return collectionRecipeRepository.findByCollectionOrderByPositionDescAddedAtDesc(collection).stream()
            .map(cr -> toRecipeCard(cr.getRecipe(), savedIds))
            .toList();
    }

    public CollectionDto getCollectionMeta(String clientId, UUID collectionId) {
        User user = resolveDeviceUser(clientId);
        RecipeCollection collection = getOwnedCollection(user, collectionId);
        Map<UUID, Integer> counts = recipeCountsByCollectionId(user);
        return toCollectionDto(collection, counts.getOrDefault(collection.getId(), 0));
    }

    public Optional<LibraryRecipeView> getRecipe(String clientId, UUID recipeId) {
        User user = resolveDeviceUser(clientId);
        return recipeRepository.findById(recipeId)
            .filter(r -> r.getUser().getId().equals(user.getId()))
            .map(r -> toLibraryRecipeView(r, user));
    }

    public CollectionDto createCollection(String clientId, String title, String description) {
        User user = resolveDeviceUser(clientId);
        ensureSystemCollections(user);

        String baseSlug = slugify(title);
        String slug = uniqueSlug(user, baseSlug);
        int sortOrder = collectionRepository.findByUserOrderBySortOrderAscCreatedAtAsc(user).stream()
            .mapToInt(RecipeCollection::getSortOrder)
            .max()
            .orElse(0) + 1;

        RecipeCollection collection = new RecipeCollection(user, title.trim(), slug, false, sortOrder);
        collection.setDescription(description != null ? description.trim() : null);
        collection = collectionRepository.save(collection);
        return toCollectionDto(collection, 0);
    }

    public boolean deleteCollection(String clientId, UUID collectionId) {
        User user = resolveDeviceUser(clientId);
        RecipeCollection collection = getOwnedCollection(user, collectionId);
        if (collection.isSystemDefault()) {
            return false;
        }
        collectionRecipeRepository.findByCollectionOrderByPositionDescAddedAtDesc(collection)
            .forEach(collectionRecipeRepository::delete);
        collectionRepository.delete(collection);
        return true;
    }

    public boolean addRecipeToCollection(String clientId, UUID collectionId, UUID recipeId) {
        User user = resolveDeviceUser(clientId);
        RecipeCollection collection = getOwnedCollection(user, collectionId);
        Recipe recipe = recipeRepository.findById(recipeId)
            .filter(r -> r.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found."));
        return addRecipeToCollection(collection, recipe);
    }

    public boolean removeRecipeFromCollection(String clientId, UUID collectionId, UUID recipeId) {
        User user = resolveDeviceUser(clientId);
        RecipeCollection collection = getOwnedCollection(user, collectionId);
        if (RecipeCollection.SLUG_MY_RECIPES.equals(collection.getSlug())) {
            return false;
        }
        Recipe recipe = recipeRepository.findById(recipeId)
            .filter(r -> r.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found."));
        if (!collectionRecipeRepository.existsByCollectionAndRecipe(collection, recipe)) {
            return false;
        }
        collectionRecipeRepository.deleteByCollectionAndRecipe(collection, recipe);
        return true;
    }

    public boolean deleteRecipe(String clientId, UUID recipeId) {
        User user = resolveDeviceUser(clientId);
        Optional<Recipe> recipeOpt = recipeRepository.findById(recipeId)
            .filter(r -> r.getUser().getId().equals(user.getId()));
        if (recipeOpt.isEmpty()) {
            return false;
        }
        Recipe recipe = recipeOpt.get();
        collectionRecipeRepository.deleteByRecipe(recipe);
        recipeRepository.delete(recipe);
        return true;
    }

    public int migrateLocalFavorites(String clientId, List<FavoriteRecipeDto> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int limit = Math.min(items.size(), 50);
        User user = resolveDeviceUser(clientId);
        ensureSystemCollections(user);
        RecipeCollection saved = getSystemCollection(user, RecipeCollection.SLUG_SAVED);
        int migrated = 0;

        for (int i = 0; i < limit; i++) {
            FavoriteRecipeDto item = items.get(i);
            if (item.getStructuredRecipe() == null) {
                continue;
            }
            try {
                StructuredRecipe structured = item.getStructuredRecipe();
                String recipeJson = objectMapper.writeValueAsString(structured);
                String contentHash = item.getContentHash();
                if (contentHash == null || contentHash.isBlank()) {
                    contentHash = com.recipeassistant.util.ContentHashUtil.sha256(recipeJson);
                }
                Recipe recipe = persistGeneratedRecipe(
                    clientId, structured, recipeJson, contentHash,
                    item.getIngredients(), item.getCuisine(), item.getDietaryRestrictions()
                );
                if (!collectionRecipeRepository.existsByCollectionAndRecipe(saved, recipe)) {
                    addRecipeToCollection(saved, recipe);
                    migrated++;
                }
            } catch (Exception ignored) {
                // skip invalid legacy entries
            }
        }
        return migrated;
    }

    public long countRecipes(String clientId) {
        User user = resolveDeviceUser(clientId);
        return recipeRepository.countByUser(user);
    }

    private void ensureSystemCollections(User user) {
        ensureCollection(user, RecipeCollection.SLUG_MY_RECIPES, "My Recipes", 0);
        ensureCollection(user, RecipeCollection.SLUG_SAVED, "Saved", 1);
    }

    private void ensureCollection(User user, String slug, String title, int sortOrder) {
        collectionRepository.findByUserAndSlug(user, slug).orElseGet(() ->
            collectionRepository.save(new RecipeCollection(user, title, slug, true, sortOrder))
        );
    }

    private RecipeCollection getSystemCollection(User user, String slug) {
        ensureSystemCollections(user);
        return collectionRepository.findByUserAndSlug(user, slug)
            .orElseThrow(() -> new IllegalStateException("Missing system collection: " + slug));
    }

    private RecipeCollection getOwnedCollection(User user, UUID collectionId) {
        RecipeCollection collection = collectionRepository.findById(collectionId)
            .orElseThrow(() -> new IllegalArgumentException("Collection not found."));
        if (!collection.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Collection not found.");
        }
        return collection;
    }

    private boolean addRecipeToCollection(RecipeCollection collection, Recipe recipe) {
        if (collectionRecipeRepository.existsByCollectionAndRecipe(collection, recipe)) {
            return false;
        }
        int position = collectionRecipeRepository.findMaxPosition(collection) + 1;
        collectionRecipeRepository.save(new CollectionRecipe(collection, recipe, position));
        return true;
    }

    private CollectionDto toCollectionDto(RecipeCollection collection, int recipeCount) {
        CollectionDto dto = new CollectionDto();
        dto.setId(collection.getId().toString());
        dto.setTitle(collection.getTitle());
        dto.setDescription(collection.getDescription());
        dto.setSlug(collection.getSlug());
        dto.setSystemDefault(collection.isSystemDefault());
        dto.setSortOrder(collection.getSortOrder());
        dto.setRecipeCount(recipeCount);
        return dto;
    }

    private Map<UUID, Integer> recipeCountsByCollectionId(User user) {
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : collectionRecipeRepository.countRecipesGroupedByCollection(user)) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private RecipeCardDto toRecipeCard(Recipe recipe, Set<UUID> savedRecipeIds) {
        RecipeCardDto dto = new RecipeCardDto();
        dto.setId(recipe.getId().toString());
        dto.setTitle(recipe.getTitle());
        dto.setIngredients(recipe.getIngredients());
        dto.setCuisine(recipe.getCuisine());
        dto.setDietaryRestrictions(recipe.getDietaryRestrictions());
        dto.setContentHash(recipe.getContentHash());
        dto.setCreatedAt(recipe.getCreatedAt() != null ? recipe.getCreatedAt().toString() : null);
        dto.setInSaved(savedRecipeIds.contains(recipe.getId()));
        return dto;
    }

    private LibraryRecipeView toLibraryRecipeView(Recipe recipe, User user) {
        StructuredRecipe structured = null;
        try {
            structured = objectMapper.readValue(recipe.getInstructions(), StructuredRecipe.class);
        } catch (Exception ignored) {
            // legacy row
        }
        Set<UUID> savedIds = collectionRecipeRepository.findRecipeIdsByCollection(
            getSystemCollection(user, RecipeCollection.SLUG_SAVED));
        return new LibraryRecipeView(
            recipe.getId(),
            structured,
            recipe.getIngredients(),
            recipe.getCuisine(),
            recipe.getDietaryRestrictions(),
            recipe.getContentHash(),
            savedIds.contains(recipe.getId())
        );
    }

    private String slugify(String title) {
        String slug = SLUG_SAFE.matcher(title.toLowerCase(Locale.ROOT).trim()).replaceAll("-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "collection" : slug.substring(0, Math.min(slug.length(), 80));
    }

    private String uniqueSlug(User user, String baseSlug) {
        String candidate = baseSlug;
        int suffix = 2;
        while (collectionRepository.findByUserAndSlug(user, candidate).isPresent()) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    public record SavedToggleResult(Recipe recipe, boolean saved) {}

    public record LibraryRecipeView(
        UUID id,
        StructuredRecipe recipe,
        String ingredients,
        String cuisine,
        String dietaryRestrictions,
        String contentHash,
        boolean inSaved
    ) {}
}
