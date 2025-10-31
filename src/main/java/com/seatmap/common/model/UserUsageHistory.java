package com.seatmap.common.model;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Tracks user usage history by month to enforce tier-based limits
 * Follows the same pattern as GuestAccessHistory for consistency
 */
public class UserUsageHistory {
    private String userId;                    // Primary key
    private String monthYear;                 // Range key: "2024-10" for monthly partitioning
    private Integer bookmarksCreated;         // Number of bookmarks created this month
    private Integer seatmapRequestsUsed;      // Number of seatmap requests made this month
    private Instant lastBookmarkCreated;      // Most recent bookmark creation
    private Instant lastSeatmapRequest;       // Most recent seatmap request
    private Instant expiresAt;                // TTL for DynamoDB cleanup (13 months)
    private Instant createdAt;                // When this record was created
    private Instant updatedAt;                // When this record was last updated
    
    public UserUsageHistory() {
        this.bookmarksCreated = 0;
        this.seatmapRequestsUsed = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        // Expire after 13 months (keep 1 year of history)
        this.expiresAt = Instant.now().plusSeconds(13 * 30 * 24 * 60 * 60);
    }
    
    public UserUsageHistory(String userId, String monthYear) {
        this();
        this.userId = userId;
        this.monthYear = monthYear;
    }
    
    public UserUsageHistory(String userId) {
        this(userId, getCurrentMonthYear());
    }
    
    /**
     * Get current month-year string in format "YYYY-MM"
     */
    @JsonIgnore
    public static String getCurrentMonthYear() {
        return YearMonth.now(ZoneOffset.UTC).toString();
    }
    
    /**
     * Record a bookmark creation
     */
    public void recordBookmarkCreation() {
        this.bookmarksCreated++;
        this.lastBookmarkCreated = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Record a seatmap request
     */
    public void recordSeatmapRequest() {
        this.seatmapRequestsUsed++;
        this.lastSeatmapRequest = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if user has exceeded bookmark limit for their tier
     */
    public boolean hasExceededBookmarkLimit(int tierLimit) {
        if (tierLimit == -1) return false; // Unlimited
        return bookmarksCreated >= tierLimit;
    }
    
    /**
     * Check if user has exceeded seatmap limit for their tier
     */
    public boolean hasExceededSeatmapLimit(int tierLimit) {
        if (tierLimit == -1) return false; // Unlimited
        return seatmapRequestsUsed >= tierLimit;
    }
    
    /**
     * Get remaining bookmarks for the tier limit
     */
    @JsonIgnore
    public int getRemainingBookmarks(int tierLimit) {
        if (tierLimit == -1) return Integer.MAX_VALUE; // Unlimited
        return Math.max(0, tierLimit - bookmarksCreated);
    }
    
    /**
     * Get remaining seatmap requests for the tier limit
     */
    @JsonIgnore
    public int getRemainingSeatmapRequests(int tierLimit) {
        if (tierLimit == -1) return Integer.MAX_VALUE; // Unlimited
        return Math.max(0, tierLimit - seatmapRequestsUsed);
    }
    
    /**
     * Check if user can create another bookmark
     */
    public boolean canCreateBookmark(int tierLimit) {
        return !hasExceededBookmarkLimit(tierLimit);
    }
    
    /**
     * Check if user can make another seatmap request
     */
    public boolean canMakeSeatmapRequest(int tierLimit) {
        return !hasExceededSeatmapLimit(tierLimit);
    }
    
    /**
     * Get user-friendly error message for bookmark limit exceeded
     */
    @JsonIgnore
    public String getBookmarkLimitMessage(int tierLimit, String tierName) {
        if (tierLimit == 0) {
            return String.format("Bookmark creation is not available for %s tier. Please upgrade to PRO or BUSINESS for bookmark access.", tierName);
        }
        return String.format("You've reached your monthly limit of %d bookmarks for %s tier. Upgrade to BUSINESS for unlimited bookmarks.", tierLimit, tierName);
    }
    
    /**
     * Get user-friendly error message for seatmap limit exceeded
     */
    @JsonIgnore
    public String getSeatmapLimitMessage(int tierLimit, String tierName) {
        return String.format("You've reached your monthly limit of %d seat map requests for %s tier. Upgrade for more access.", tierLimit, tierName);
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
    
    public Integer getBookmarksCreated() { return bookmarksCreated; }
    public void setBookmarksCreated(Integer bookmarksCreated) { this.bookmarksCreated = bookmarksCreated; }
    
    public Integer getSeatmapRequestsUsed() { return seatmapRequestsUsed; }
    public void setSeatmapRequestsUsed(Integer seatmapRequestsUsed) { this.seatmapRequestsUsed = seatmapRequestsUsed; }
    
    public Instant getLastBookmarkCreated() { return lastBookmarkCreated; }
    public void setLastBookmarkCreated(Instant lastBookmarkCreated) { this.lastBookmarkCreated = lastBookmarkCreated; }
    
    public Instant getLastSeatmapRequest() { return lastSeatmapRequest; }
    public void setLastSeatmapRequest(Instant lastSeatmapRequest) { this.lastSeatmapRequest = lastSeatmapRequest; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}