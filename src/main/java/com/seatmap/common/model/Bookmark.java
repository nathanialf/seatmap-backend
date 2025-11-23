package com.seatmap.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.seatmap.api.model.FlightSearchRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    
    // BOOKMARK item field
    private String flightOfferData;           // For BOOKMARK type - JSON string
    
    // SAVED_SEARCH item fields (top-level DynamoDB columns)
    @Pattern(regexp = "^[A-Z]{3}$", message = "Origin must be a 3-letter airport code")
    private String origin;
    
    @Pattern(regexp = "^[A-Z]{3}$", message = "Destination must be a 3-letter airport code") 
    private String destination;
    
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Departure date must be in YYYY-MM-DD format")
    private String departureDate;
    
    @Pattern(regexp = "^(ECONOMY|PREMIUM_ECONOMY|BUSINESS|FIRST)$", message = "Travel class must be ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST")
    private String travelClass; // Optional - allow null
    
    @Pattern(regexp = "^[A-Z]{2,3}$", message = "Airline code must be 2-3 uppercase letters")
    private String airlineCode; // Optional - allow null
    
    @Pattern(regexp = "^[0-9]{1,4}$", message = "Flight number must be 1-4 digits")
    private String flightNumber; // Optional - allow null
    
    @Min(value = 1, message = "Max results must be at least 1")
    @Max(value = 50, message = "Max results cannot exceed 50")
    private Integer maxResults; // Optional - allow null
    
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
        }
    }
    
    // Constructor for saved search items
    public Bookmark(String userId, String bookmarkId, String title, FlightSearchRequest searchRequest, ItemType itemType) {
        this();
        this.userId = userId;
        this.bookmarkId = bookmarkId;
        this.title = title;
        this.itemType = itemType;
        
        if (itemType == ItemType.SAVED_SEARCH) {
            this.origin = searchRequest.getOrigin();
            this.destination = searchRequest.getDestination();
            this.departureDate = searchRequest.getDepartureDate();
            this.travelClass = searchRequest.getTravelClass();
            this.airlineCode = searchRequest.getAirlineCode();
            this.flightNumber = searchRequest.getFlightNumber();
            this.maxResults = searchRequest.getMaxResults();
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

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public String getTravelClass() {
        return travelClass;
    }

    public void setTravelClass(String travelClass) {
        this.travelClass = travelClass;
    }

    public String getAirlineCode() {
        return airlineCode;
    }

    public void setAirlineCode(String airlineCode) {
        this.airlineCode = airlineCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
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
    
    // Helper method to reconstruct FlightSearchRequest from SAVED_SEARCH fields
    @JsonIgnore
    public FlightSearchRequest toFlightSearchRequest() {
        if (itemType != ItemType.SAVED_SEARCH) {
            return null;
        }
        FlightSearchRequest request = new FlightSearchRequest();
        request.setOrigin(this.origin);
        request.setDestination(this.destination);
        request.setDepartureDate(this.departureDate);
        request.setTravelClass(this.travelClass);
        request.setAirlineCode(this.airlineCode);
        request.setFlightNumber(this.flightNumber);
        request.setMaxResults(this.maxResults);
        return request;
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