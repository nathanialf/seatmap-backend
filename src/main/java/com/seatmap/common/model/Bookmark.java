package com.seatmap.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class Bookmark {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotBlank(message = "Bookmark ID is required")
    private String bookmarkId;
    
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be 100 characters or less")
    private String title;
    
    @NotBlank(message = "Flight offer data is required")
    private String flightOfferData;
    
    @NotBlank(message = "Source is required")
    private String source;
    
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;

    public Bookmark() {}

    public Bookmark(String userId, String bookmarkId, String title, String flightOfferData, String source) {
        this.userId = userId;
        this.bookmarkId = bookmarkId;
        this.title = title;
        this.flightOfferData = flightOfferData;
        this.source = source;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    @JsonProperty("isExpired")
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "Bookmark{" +
                "userId='" + userId + '\'' +
                ", bookmarkId='" + bookmarkId + '\'' +
                ", title='" + title + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}