package com.seatmap.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public class TierDefinition {
    private String tierId;
    private String tierName;
    private String displayName;
    private String description;
    private Integer maxBookmarks;
    private Integer maxSeatmapCalls;
    private BigDecimal priceUsd;
    private String billingType; // "one_time", "monthly", "annual"
    private Boolean canDowngrade;
    private String region;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean active;

    public TierDefinition() {
        this.active = true;
        this.region = "US";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getTierId() { return tierId; }
    public void setTierId(String tierId) { this.tierId = tierId; }

    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getMaxBookmarks() { return maxBookmarks; }
    public void setMaxBookmarks(Integer maxBookmarks) { this.maxBookmarks = maxBookmarks; }

    public Integer getMaxSeatmapCalls() { return maxSeatmapCalls; }
    public void setMaxSeatmapCalls(Integer maxSeatmapCalls) { this.maxSeatmapCalls = maxSeatmapCalls; }

    public BigDecimal getPriceUsd() { return priceUsd; }
    public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }

    public String getBillingType() { return billingType; }
    public void setBillingType(String billingType) { this.billingType = billingType; }

    public Boolean getCanDowngrade() { return canDowngrade != null ? canDowngrade : true; }
    public void setCanDowngrade(Boolean canDowngrade) { this.canDowngrade = canDowngrade; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getActive() { return active != null ? active : true; }
    public void setActive(Boolean active) { this.active = active; }

    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}