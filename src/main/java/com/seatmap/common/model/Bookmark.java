package com.seatmap.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.seatmap.api.model.FlightSearchRequest;
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
    private String flightOfferData;           // For BOOKMARK type
    private FlightSearchRequest searchRequest; // For SAVED_SEARCH type
    
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
    public Bookmark(String userId, String bookmarkId, String title, String flightOfferData) {
        this();
        this.userId = userId;
        this.bookmarkId = bookmarkId;
        this.title = title;
        this.itemType = ItemType.BOOKMARK;
        this.flightOfferData = flightOfferData;
    }
    
    // Constructor for saved search
    public Bookmark(String userId, String bookmarkId, String title, FlightSearchRequest searchRequest) {
        this();
        this.userId = userId;
        this.bookmarkId = bookmarkId;
        this.title = title;
        this.itemType = ItemType.SAVED_SEARCH;
        this.searchRequest = searchRequest;
        
        // Set expiration to flight departure date
        if (searchRequest != null && searchRequest.getDepartureDate() != null) {
            try {
                // Parse departure date and set expiration to end of that day
                java.time.LocalDate departureDate = java.time.LocalDate.parse(searchRequest.getDepartureDate());
                this.expiresAt = departureDate.atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant();
            } catch (Exception e) {
                // If parsing fails, fall back to 30 days
                this.expiresAt = Instant.now().plusSeconds(30 * 24 * 60 * 60);
            }
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

    public FlightSearchRequest getSearchRequest() {
        return searchRequest;
    }

    public void setSearchRequest(FlightSearchRequest searchRequest) {
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