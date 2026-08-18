package com.recipeassistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trial_client_usage")
public class TrialClientUsage {

    @Id
    @Column(name = "client_id", length = 36)
    private String clientId;

    @Column(name = "recipe_count", nullable = false)
    private int recipeCount = 0;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public TrialClientUsage() {}

    public TrialClientUsage(String clientId, String ipHash) {
        this.clientId = clientId;
        this.ipHash = ipHash;
        this.updatedAt = LocalDateTime.now();
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public int getRecipeCount() { return recipeCount; }
    public void setRecipeCount(int recipeCount) { this.recipeCount = recipeCount; }

    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
