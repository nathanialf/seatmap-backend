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
    private AlertConfig alertConfig;

    public static class AlertConfig {
        private Double alertThreshold; // null = no alert, value = alert enabled
        private Instant lastEvaluated;
        private Instant lastTriggered;
        private String triggerHistory; // JSON string of trigger events
        
        public AlertConfig() {}
        
        public AlertConfig(Double alertThreshold) {
            this.alertThreshold = alertThreshold;
        }
        
        public Double getAlertThreshold() {
            return alertThreshold;
        }
        
        public void setAlertThreshold(Double alertThreshold) {
            this.alertThreshold = alertThreshold;
        }
        
        public Instant getLastEvaluated() {
            return lastEvaluated;
        }
        
        public void setLastEvaluated(Instant lastEvaluated) {
            this.lastEvaluated = lastEvaluated;
        }
        
        public Instant getLastTriggered() {
            return lastTriggered;
        }
        
        public void setLastTriggered(Instant lastTriggered) {
            this.lastTriggered = lastTriggered;
        }
        
        public String getTriggerHistory() {
            return triggerHistory;
        }
        
        public void setTriggerHistory(String triggerHistory) {
            this.triggerHistory = triggerHistory;
        }
        
        @JsonIgnore
        public boolean isEnabled() {
            return alertThreshold != null;
        }
        
        public void updateLastEvaluated() {
            this.lastEvaluated = Instant.now();
        }
        
        public void recordTrigger() {
            this.lastTriggered = Instant.now();
        }
    }

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
            // Set expiration to 1 day after flight departure time
            this.expiresAt = extractFlightDepartureTime(flightOfferData);
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
            // Set expiration to 1 day after departure date
            this.expiresAt = parseDepartureDateExpiration(searchRequest.getDepartureDate());
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
    
    public AlertConfig getAlertConfig() {
        return alertConfig;
    }
    
    public void setAlertConfig(AlertConfig alertConfig) {
        this.alertConfig = alertConfig;
    }
    
    @JsonIgnore
    @JsonProperty("hasAlert")
    public boolean hasAlert() {
        return alertConfig != null && alertConfig.isEnabled();
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
    
    /**
     * Extract flight departure time from flight offer data and add 1 day for expiration
     */
    private Instant extractFlightDepartureTime(String flightOfferData) {
        try {
            if (flightOfferData == null || flightOfferData.trim().isEmpty()) {
                // Fallback: expire in 7 days if we can't parse flight data
                return Instant.now().plusSeconds(7 * 24 * 60 * 60);
            }
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode flightData = mapper.readTree(flightOfferData);
            
            com.fasterxml.jackson.databind.JsonNode itineraries = flightData.get("itineraries");
            if (itineraries != null && itineraries.isArray() && itineraries.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode firstItinerary = itineraries.get(0);
                com.fasterxml.jackson.databind.JsonNode segments = firstItinerary.get("segments");
                if (segments != null && segments.isArray() && segments.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode firstSegment = segments.get(0);
                    com.fasterxml.jackson.databind.JsonNode departure = firstSegment.get("departure");
                    if (departure != null) {
                        com.fasterxml.jackson.databind.JsonNode atNode = departure.get("at");
                        if (atNode != null) {
                            String departureTimeStr = atNode.asText();
                            
                            // Parse the departure time and add 1 day
                            if (departureTimeStr.endsWith("Z")) {
                                // ISO format with Z
                                Instant departureTime = Instant.parse(departureTimeStr);
                                return departureTime.plusSeconds(24 * 60 * 60); // Add 1 day
                            } else if (departureTimeStr.length() >= 19) {
                                // ISO format without Z, add Z for parsing
                                if (!departureTimeStr.contains("Z") && !departureTimeStr.contains("+")) {
                                    departureTimeStr += "Z";
                                }
                                Instant departureTime = Instant.parse(departureTimeStr);
                                return departureTime.plusSeconds(24 * 60 * 60); // Add 1 day
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log the error but don't fail - use fallback expiration
            System.err.println("Error parsing flight departure time: " + e.getMessage());
        }
        
        // Fallback: expire in 7 days if we can't parse the flight data
        return Instant.now().plusSeconds(7 * 24 * 60 * 60);
    }
    
    /**
     * Parse departure date string (YYYY-MM-DD) and add 1 day for expiration
     */
    private Instant parseDepartureDateExpiration(String departureDate) {
        try {
            if (departureDate == null || !departureDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                // Fallback: expire in 30 days if invalid date format
                return Instant.now().plusSeconds(30 * 24 * 60 * 60);
            }
            
            // Parse YYYY-MM-DD and set to end of next day (departure date + 1 day)
            java.time.LocalDate date = java.time.LocalDate.parse(departureDate);
            java.time.LocalDateTime endOfNextDay = date.plusDays(1).atTime(23, 59, 59);
            return endOfNextDay.atZone(java.time.ZoneOffset.UTC).toInstant();
            
        } catch (Exception e) {
            System.err.println("Error parsing departure date: " + e.getMessage());
            // Fallback: expire in 30 days
            return Instant.now().plusSeconds(30 * 24 * 60 * 60);
        }
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