package com.seatmap.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class User {
    private String userId;
    private String email;
    
    @JsonIgnore
    private String passwordHash;
    
    private String firstName;
    private String lastName;
    private AuthProvider authProvider;
    private String oauthId;
    private String profilePicture;
    private Instant createdAt;
    private Instant updatedAt;
    private UserStatus status;
    
    // Email verification fields
    private Boolean emailVerified = false;  // Visible in JSON responses
    @JsonIgnore
    private String verificationToken;       // Hidden from JSON
    @JsonIgnore
    private Instant verificationExpiresAt;  // Hidden from JSON

    public enum AuthProvider {
        EMAIL, GOOGLE, APPLE
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED
    }

    public User() {
        this.status = UserStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }

    public String getOauthId() { return oauthId; }
    public void setOauthId(String oauthId) { this.oauthId = oauthId; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public Boolean getEmailVerified() { return emailVerified != null ? emailVerified : false; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public Instant getVerificationExpiresAt() { return verificationExpiresAt; }
    public void setVerificationExpiresAt(Instant verificationExpiresAt) { this.verificationExpiresAt = verificationExpiresAt; }

    @JsonProperty("fullName")
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}