package com.seatmap.common.model;

import java.time.Instant;

public class Session {
    private String sessionId;
    private String userId;
    private String jwtToken;
    private UserType userType;
    private Integer guestFlightsViewed;
    private Instant createdAt;
    private Instant expiresAt;
    private String ipAddress;
    private String userAgent;

    public enum UserType {
        USER, GUEST
    }

    public Session() {
        this.createdAt = Instant.now();
        this.guestFlightsViewed = 0;
        // Set expiration to 24 hours from creation
        this.expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
    }

    public Session(String sessionId, String userId, UserType userType) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.userType = userType;
    }

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }

    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }

    public Integer getGuestFlightsViewed() { return guestFlightsViewed; }
    public void setGuestFlightsViewed(Integer guestFlightsViewed) { this.guestFlightsViewed = guestFlightsViewed; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public boolean isGuest() {
        return UserType.GUEST.equals(userType);
    }

    public boolean canViewMoreFlights() {
        if (!isGuest()) {
            return true;
        }
        return guestFlightsViewed < 2;
    }

    public void incrementGuestFlightsViewed() {
        if (isGuest()) {
            this.guestFlightsViewed = (this.guestFlightsViewed == null) ? 1 : this.guestFlightsViewed + 1;
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}