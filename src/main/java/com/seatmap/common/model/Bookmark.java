package com.seatmap.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class Bookmark {
    
    public enum ItemType {
        BOOKMARK, SAVED_SEARCH
    }
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Bookmark ID is required")
    private String bookmarkId;
    
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be 100 characters or less")
    private String title;
    
    @NotNull(message = "Item type is required")
    private ItemType itemType = ItemType.BOOKMARK; // Default for backward compatibility
    
    // Type-specific fields - only one should be populated based on itemType
    private String flightOfferData;           // For BOOKMARK type - JSON string
    private String searchRequest;             // For SAVED_SEARCH type - JSON string
    
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private Instant lastAccessedAt;

    public Bookmark() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.lastAccessedAt = Instant.now();
    }

    // Constructor for flight bookmark
    public Bookmark(String userId, String bookmarkId, String title, String flightOfferData, ItemType itemType) {
        this();
        this.userId = userId;
        this.bookmarkId = bookmarkId;
        this.title = title;
        this.itemType = itemType;
        
        if (itemType == ItemType.BOOKMARK) {
            this.flightOfferData = flightOfferData;
        } else if (itemType == ItemType.SAVED_SEARCH) {
            this.searchRequest = flightOfferData; // flightOfferData parameter is actually searchRequest JSON
            // Set expiration to 30 days by default for saved searches
            this.expiresAt = Instant.now().plusSeconds(30 * 24 * 60 * 60);
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBookmarkId() {
        return bookmarkId;
    }

    public void setBookmarkId(String bookmarkId) {
        this.bookmarkId = bookmarkId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFlightOfferData() {
        return flightOfferData;
    }

    public void setFlightOfferData(String flightOfferData) {
        this.flightOfferData = flightOfferData;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(String searchRequest) {
        this.searchRequest = searchRequest;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @JsonIgnore  // Don't serialize to DynamoDB
    @JsonProperty("isExpired")  // But include in API responses
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @JsonIgnore  // Don't serialize to DynamoDB
    @JsonProperty("isBookmark")  // But include in API responses
    public boolean isBookmark() {
        return itemType == ItemType.BOOKMARK;
    }

    @JsonIgnore  // Don't serialize to DynamoDB
    @JsonProperty("isSavedSearch")  // But include in API responses
    public boolean isSavedSearch() {
        return itemType == ItemType.SAVED_SEARCH;
    }

    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "Bookmark{" +
                "userId='" + userId + '\'' +
                ", bookmarkId='" + bookmarkId + '\'' +
                ", title='" + title + '\'' +
                ", itemType=" + itemType +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", expiresAt=" + expiresAt +
                ", lastAccessedAt=" + lastAccessedAt +
                '}';
    }
}