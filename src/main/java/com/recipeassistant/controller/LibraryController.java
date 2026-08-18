package com.recipeassistant.controller;

import com.recipeassistant.model.*;
import com.recipeassistant.service.RecipeLibraryService;
import com.recipeassistant.service.TrialClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/library")
public class LibraryController {

    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);
    private static final String GENERIC_ERROR = "An error occurred. Please try again later.";

    @Autowired private RecipeLibraryService libraryService;
    @Autowired private TrialClientService trialClientService;

    @GetMapping("/collections")
    public ResponseEntity<RecipeResponse> listCollections(HttpServletRequest request, HttpServletResponse response) {
        try {
            String clientId = trialClientService.ensureClientId(request, response);
            RecipeResponse body = new RecipeResponse(true, null, null);
            body.setCollections(libraryService.listCollections(clientId));
            body.setTotalRecipes((int) libraryService.countRecipes(clientId));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("List collections failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @PostMapping("/collections")
    public ResponseEntity<RecipeResponse> createCollection(@Valid @RequestBody CreateCollectionRequest request,
                                                           BindingResult bindingResult,
                                                           HttpServletRequest httpRequest,
                                                           HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, "Invalid collection."));
        }
        try {
            String clientId = trialClientService.ensureClientId(httpRequest, httpResponse);
            CollectionDto created = libraryService.createCollection(
                clientId, request.getTitle(), request.getDescription());
            RecipeResponse body = new RecipeResponse(true, "Collection created.", null);
            body.setActiveCollection(created);
            body.setCollections(libraryService.listCollections(clientId));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Create collection failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @GetMapping("/collections/{id}")
    public ResponseEntity<RecipeResponse> getCollection(@PathVariable("id") UUID id,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        try {
            String clientId = trialClientService.ensureClientId(request, response);
            RecipeResponse body = new RecipeResponse(true, null, null);
            body.setActiveCollection(libraryService.getCollectionMeta(clientId, id));
            body.setRecipeCards(libraryService.listCollectionRecipes(clientId, id));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Get collection failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @DeleteMapping("/collections/{id}")
    public ResponseEntity<RecipeResponse> deleteCollection(@PathVariable("id") UUID id,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        try {
            String clientId = trialClientService.ensureClientId(request, response);
            if (!libraryService.deleteCollection(clientId, id)) {
                return ResponseEntity.badRequest()
                    .body(new RecipeResponse(false, null, "Cannot delete this collection."));
            }
            RecipeResponse body = new RecipeResponse(true, "Collection deleted.", null);
            body.setCollections(libraryService.listCollections(clientId));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Delete collection failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @PostMapping("/collections/{id}/recipes")
    public ResponseEntity<RecipeResponse> addRecipeToCollection(@PathVariable("id") UUID id,
                                                                  @Valid @RequestBody LibraryRecipeRequest request,
                                                                  BindingResult bindingResult,
                                                                  HttpServletRequest httpRequest,
                                                                  HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, "Invalid recipe id."));
        }
        try {
            String clientId = trialClientService.ensureClientId(httpRequest, httpResponse);
            libraryService.addRecipeToCollection(clientId, id, UUID.fromString(request.getRecipeId()));
            RecipeResponse body = new RecipeResponse(true, "Added to collection.", null);
            body.setActiveCollection(libraryService.getCollectionMeta(clientId, id));
            body.setRecipeCards(libraryService.listCollectionRecipes(clientId, id));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Add to collection failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @DeleteMapping("/collections/{collectionId}/recipes/{recipeId}")
    public ResponseEntity<RecipeResponse> removeRecipeFromCollection(@PathVariable UUID collectionId,
                                                                     @PathVariable UUID recipeId,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) {
        try {
            String clientId = trialClientService.ensureClientId(request, response);
            if (!libraryService.removeRecipeFromCollection(clientId, collectionId, recipeId)) {
                return ResponseEntity.badRequest()
                    .body(new RecipeResponse(false, null, "Cannot remove from this collection."));
            }
            RecipeResponse body = new RecipeResponse(true, "Removed from collection.", null);
            body.setActiveCollection(libraryService.getCollectionMeta(clientId, collectionId));
            body.setRecipeCards(libraryService.listCollectionRecipes(clientId, collectionId));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Remove from collection failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @GetMapping("/recipes/{id}")
    public ResponseEntity<RecipeResponse> getRecipe(@PathVariable("id") UUID id,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        try {
            String clientId = trialClientService.ensureClientId(request, response);
            return libraryService.getRecipe(clientId, id)
                .map(view -> {
                    RecipeResponse body = new RecipeResponse(true, null, null);
                    body.setRecipeId(view.id().toString());
                    body.setRecipe(view.recipe());
                    body.setContentHash(view.contentHash());
                    body.setFavorited(view.inSaved());
                    body.setSavedIngredients(view.ingredients());
                    body.setSavedCuisine(view.cuisine());
                    body.setSavedDietaryRestrictions(view.dietaryRestrictions());
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> ResponseEntity.badRequest()
                    .body(new RecipeResponse(false, null, "Recipe not found.")));
        } catch (Exception e) {
            log.error("Get library recipe failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @DeleteMapping("/recipes/{id}")
    public ResponseEntity<RecipeResponse> deleteRecipe(@PathVariable("id") UUID id,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        try {
            String clientId = trialClientService.ensureClientId(request, response);
            if (!libraryService.deleteRecipe(clientId, id)) {
                return ResponseEntity.badRequest().body(new RecipeResponse(false, null, "Recipe not found."));
            }
            RecipeResponse body = new RecipeResponse(true, "Recipe deleted.", null);
            body.setCollections(libraryService.listCollections(clientId));
            body.setTotalRecipes((int) libraryService.countRecipes(clientId));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Delete recipe failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @PostMapping("/saved/toggle")
    public ResponseEntity<RecipeResponse> toggleSaved(@Valid @RequestBody LibraryRecipeRequest request,
                                                      BindingResult bindingResult,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, "Invalid recipe id."));
        }
        try {
            String clientId = trialClientService.ensureClientId(httpRequest, httpResponse);
            RecipeLibraryService.SavedToggleResult result = libraryService.toggleSaved(
                clientId, UUID.fromString(request.getRecipeId()));
            RecipeResponse body = new RecipeResponse(true,
                result.saved() ? "Saved to your cookbook." : "Removed from Saved.", null);
            body.setRecipeId(result.recipe().getId().toString());
            body.setFavorited(result.saved());
            body.setContentHash(result.recipe().getContentHash());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new RecipeResponse(false, null, e.getMessage()));
        } catch (Exception e) {
            log.error("Toggle saved failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }

    @PostMapping("/migrate-local")
    public ResponseEntity<RecipeResponse> migrateLocal(@Valid @RequestBody MigrateLocalLibraryRequest request,
                                                       BindingResult bindingResult,
                                                       HttpServletRequest httpRequest,
                                                       HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                .body(new RecipeResponse(false, null, "Too many recipes to import at once (max 50)."));
        }
        try {
            String clientId = trialClientService.ensureClientId(httpRequest, httpResponse);
            int migrated = libraryService.migrateLocalFavorites(clientId, request.getFavorites());
            RecipeResponse body = new RecipeResponse(true,
                migrated > 0 ? "Imported " + migrated + " recipe(s) into your library." : "Nothing to import.", null);
            body.setCollections(libraryService.listCollections(clientId));
            body.setTotalRecipes((int) libraryService.countRecipes(clientId));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Migrate local library failed", e);
            return ResponseEntity.internalServerError().body(new RecipeResponse(false, null, GENERIC_ERROR));
        }
    }
}
