package com.recipeassistant.repository;

import com.recipeassistant.model.Recipe;
import com.recipeassistant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    
    /**
     * Find recipes by user
     */
    List<Recipe> findByUserOrderByCreatedAtDesc(User user);

    Optional<Recipe> findByUserAndContentHash(User user, String contentHash);
    
    /**
     * Find recipes by cuisine and user
     */
    List<Recipe> findByCuisineIgnoreCaseAndUserOrderByCreatedAtDesc(String cuisine, User user);
    
    /**
     * Search recipes by title (case-insensitive)
     */
    @Query("SELECT r FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND r.user = :user")
    List<Recipe> searchByTitleAndUser(@Param("searchTerm") String searchTerm, @Param("user") User user);
    
    
    /**
     * Count recipes by user
     */
    long countByUser(User user);
}
