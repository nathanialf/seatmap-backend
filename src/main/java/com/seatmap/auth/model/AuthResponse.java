package com.seatmap.auth.model;

import com.seatmap.common.model.User;

public class AuthResponse {
    private String token;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private User.AuthProvider authProvider;
    private String profilePicture;
    private boolean isNewUser;
    private int expiresIn;
    private GuestLimits guestLimits;
    private String message;

    public AuthResponse() {}

    public AuthResponse(String token, User user, int expiresIn) {
        this.token = token;
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.authProvider = user.getAuthProvider();
        this.profilePicture = user.getProfilePicture();
        this.expiresIn = expiresIn;
        this.isNewUser = false;
    }

    public static AuthResponse forGuest(String token, String sessionId, int expiresIn) {
        AuthResponse response = new AuthResponse();
        response.token = token;
        response.userId = sessionId;
        response.expiresIn = expiresIn;
        response.guestLimits = new GuestLimits(0, 2);
        return response;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public User.AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(User.AuthProvider authProvider) { this.authProvider = authProvider; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public boolean isNewUser() { return isNewUser; }
    public void setNewUser(boolean newUser) { isNewUser = newUser; }

    public int getExpiresIn() { return expiresIn; }
    public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }

    public GuestLimits getGuestLimits() { return guestLimits; }
    public void setGuestLimits(GuestLimits guestLimits) { this.guestLimits = guestLimits; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class GuestLimits {
        private int flightsViewed;
        private int maxFlights;

        public GuestLimits() {}

        public GuestLimits(int flightsViewed, int maxFlights) {
            this.flightsViewed = flightsViewed;
            this.maxFlights = maxFlights;
        }

        public int getFlightsViewed() { return flightsViewed; }
        public void setFlightsViewed(int flightsViewed) { this.flightsViewed = flightsViewed; }

        public int getMaxFlights() { return maxFlights; }
        public void setMaxFlights(int maxFlights) { this.maxFlights = maxFlights; }
    }
}