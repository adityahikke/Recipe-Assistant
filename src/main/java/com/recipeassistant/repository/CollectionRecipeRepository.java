package com.recipeassistant.repository;

import com.recipeassistant.model.CollectionRecipe;
import com.recipeassistant.model.Recipe;
import com.recipeassistant.model.RecipeCollection;
import com.recipeassistant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CollectionRecipeRepository extends JpaRepository<CollectionRecipe, UUID> {

    boolean existsByCollectionAndRecipe(RecipeCollection collection, Recipe recipe);

    Optional<CollectionRecipe> findByCollectionAndRecipe(RecipeCollection collection, Recipe recipe);

    List<CollectionRecipe> findByCollectionOrderByPositionDescAddedAtDesc(RecipeCollection collection);

    long countByCollection(RecipeCollection collection);

    void deleteByCollectionAndRecipe(RecipeCollection collection, Recipe recipe);

    void deleteByRecipe(Recipe recipe);

    @Query("SELECT cr.recipe.id FROM CollectionRecipe cr WHERE cr.collection = :collection")
    Set<UUID> findRecipeIdsByCollection(@Param("collection") RecipeCollection collection);

    @Query("SELECT cr.collection.id, COUNT(cr) FROM CollectionRecipe cr WHERE cr.collection.user = :user GROUP BY cr.collection.id")
    List<Object[]> countRecipesGroupedByCollection(@Param("user") User user);

    @Query("SELECT COALESCE(MAX(cr.position), 0) FROM CollectionRecipe cr WHERE cr.collection = :collection")
    int findMaxPosition(@Param("collection") RecipeCollection collection);
}
