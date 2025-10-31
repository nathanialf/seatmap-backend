package com.seatmap.common.model;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Tracks guest access history by IP address to enforce seatmap request limiting
 * Allows 2 seatmap requests per 30 days per IP
 */
public class GuestAccessHistory {
    private String ipAddress;              // Primary key
    private Integer seatmapRequestsUsed;   // Number of seatmap requests made (lifetime)
    private Instant firstAccess;          // When this IP first used guest access
    private Instant lastSeatmapRequest;   // Most recent seatmap request
    private Instant expiresAt;            // TTL for DynamoDB cleanup (6 months)
    
    public GuestAccessHistory() {
        this.seatmapRequestsUsed = 0;
        this.firstAccess = Instant.now();
        // Expire after 30 days (TTL for cleanup)
        this.expiresAt = Instant.now().plusSeconds(30 * 24 * 60 * 60);
    }
    
    public GuestAccessHistory(String ipAddress) {
        this();
        this.ipAddress = ipAddress;
    }
    
    /**
     * Check if this IP has exceeded the seatmap request limit (2 per 30 days)
     */
    public boolean hasExceededSeatmapLimit() {
        return seatmapRequestsUsed >= 2;
    }
    
    /**
     * Get remaining seatmap requests for this IP
     */
    @JsonIgnore
    public int getRemainingSeatmapRequests() {
        return Math.max(0, 2 - seatmapRequestsUsed);
    }
    
    /**
     * Record a seatmap request
     */
    public void recordSeatmapRequest() {
        this.seatmapRequestsUsed++;
        this.lastSeatmapRequest = Instant.now();
        
        // Extend TTL on activity (30 days)
        this.expiresAt = Instant.now().plusSeconds(30 * 24 * 60 * 60);
    }
    
    /**
     * Check if this IP can make another seatmap request
     */
    public boolean canMakeSeatmapRequest() {
        return !hasExceededSeatmapLimit();
    }
    
    /**
     * Get a user-friendly error message for why seatmap access is denied
     */
    @JsonIgnore
    public String getSeatmapDenialMessage() {
        if (hasExceededSeatmapLimit()) {
            return String.format("You've used your %d free seat map views. Please register for unlimited seat map access.", 2);
        }
        
        return "Seat map access is temporarily unavailable. Please register for unlimited access.";
    }
    
    // Getters and Setters
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public Integer getSeatmapRequestsUsed() { return seatmapRequestsUsed; }
    public void setSeatmapRequestsUsed(Integer seatmapRequestsUsed) { this.seatmapRequestsUsed = seatmapRequestsUsed; }
    
    public Instant getFirstAccess() { return firstAccess; }
    public void setFirstAccess(Instant firstAccess) { this.firstAccess = firstAccess; }
    
    public Instant getLastSeatmapRequest() { return lastSeatmapRequest; }
    public void setLastSeatmapRequest(Instant lastSeatmapRequest) { this.lastSeatmapRequest = lastSeatmapRequest; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}