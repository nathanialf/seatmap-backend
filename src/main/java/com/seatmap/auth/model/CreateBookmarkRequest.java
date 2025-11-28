package com.seatmap.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.common.model.Bookmark;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateBookmarkRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be 100 characters or less")
    private String title;
    
    // For regular bookmarks - flight offer data JSON string
    private String flightOfferData;
    
    // For saved searches - flight search criteria
    @Valid
    private FlightSearchRequest searchRequest;
    
    // Item type - REQUIRED, no default
    @NotNull(message = "Item type is required")
    private Bookmark.ItemType itemType;
    
    // Optional alert configuration
    private Bookmark.AlertConfig alertConfig;

    public CreateBookmarkRequest() {}

    // Constructor for regular bookmarks
    public CreateBookmarkRequest(String title, String flightOfferData, Bookmark.ItemType itemType) {
        this.title = title;
        this.flightOfferData = flightOfferData;
        this.itemType = itemType;
    }
    
    // Constructor for saved searches
    public CreateBookmarkRequest(String title, FlightSearchRequest searchRequest, Bookmark.ItemType itemType) {
        this.title = title;
        this.searchRequest = searchRequest;
        this.itemType = itemType;
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
    
    public FlightSearchRequest getSearchRequest() {
        return searchRequest;
    }
    
    public void setSearchRequest(FlightSearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }
    
    public Bookmark.ItemType getItemType() {
        return itemType;
    }
    
    public void setItemType(Bookmark.ItemType itemType) {
        this.itemType = itemType;
    }
    
    public Bookmark.AlertConfig getAlertConfig() {
        return alertConfig;
    }
    
    public void setAlertConfig(Bookmark.AlertConfig alertConfig) {
        this.alertConfig = alertConfig;
    }
    
    /**
     * Validate that the request has the appropriate data for its item type
     */
    @JsonIgnore
    public boolean isValid() {
        if (itemType == null) {
            return false;
        }
        
        if (itemType == Bookmark.ItemType.BOOKMARK) {
            return flightOfferData != null && !flightOfferData.trim().isEmpty();
        } else if (itemType == Bookmark.ItemType.SAVED_SEARCH) {
            return searchRequest != null;
        }
        return false;
    }
    
    /**
     * Get validation error message for invalid requests
     */
    @JsonIgnore
    public String getValidationError() {
        if (itemType == null) {
            return "Item type is required";
        }
        
        if (itemType == Bookmark.ItemType.BOOKMARK) {
            if (flightOfferData == null || flightOfferData.trim().isEmpty()) {
                return "Flight offer data is required for bookmark items";
            }
        } else if (itemType == Bookmark.ItemType.SAVED_SEARCH) {
            if (searchRequest == null) {
                return "Search request is required for saved search items";
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "CreateBookmarkRequest{" +
                "title='" + title + '\'' +
                ", itemType=" + itemType +
                ", hasFlightOfferData=" + (flightOfferData != null) +
                ", hasSearchRequest=" + (searchRequest != null) +
                ", hasAlertConfig=" + (alertConfig != null) +
                '}';
    }
}