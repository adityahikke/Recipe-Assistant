package com.recipeassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collections", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "slug"})
})
public class RecipeCollection {

    public static final String SLUG_MY_RECIPES = "my-recipes";
    public static final String SLUG_SAVED = "saved";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(name = "is_system_default", nullable = false)
    private boolean systemDefault = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RecipeCollection() {}

    public RecipeCollection(User user, String title, String slug, boolean systemDefault, int sortOrder) {
        this.user = user;
        this.title = title;
        this.slug = slug;
        this.systemDefault = systemDefault;
        this.sortOrder = sortOrder;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public boolean isSystemDefault() { return systemDefault; }
    public void setSystemDefault(boolean systemDefault) { this.systemDefault = systemDefault; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
