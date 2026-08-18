package com.recipeassistant.repository;

import com.recipeassistant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    Optional<User> findByClientId(String clientId);
    
    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);
    
}
