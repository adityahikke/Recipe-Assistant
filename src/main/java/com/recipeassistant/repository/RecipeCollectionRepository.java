package com.recipeassistant.repository;

import com.recipeassistant.model.RecipeCollection;
import com.recipeassistant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipeCollectionRepository extends JpaRepository<RecipeCollection, UUID> {

    Optional<RecipeCollection> findByUserAndSlug(User user, String slug);

    List<RecipeCollection> findByUserOrderBySortOrderAscCreatedAtAsc(User user);
}
