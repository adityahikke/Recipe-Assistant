package com.recipeassistant.repository;

import com.recipeassistant.model.TrialClientUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrialClientUsageRepository extends JpaRepository<TrialClientUsage, String> {
}
