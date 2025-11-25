package com.seatmap.auth.service;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    
    private JwtService jwtService;
    
    @BeforeEach
    void setUp() {
        // Set a test JWT secret
        System.setProperty("JWT_SECRET", "test-secret-key-that-is-at-least-32-characters-long-for-testing");
        jwtService = new JwtService();
    }
    
    @Test
    @DisplayName("Should generate valid token for user")
    void shouldGenerateValidTokenForUser() throws SeatmapException {
        User user = createTestUser();
        
        String token = jwtService.generateToken(user);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Validate the token
        Claims claims = jwtService.validateToken(token);
        assertEquals(user.getUserId(), claims.getSubject());
        assertEquals(user.getEmail(), claims.get("email"));
        assertEquals("user", claims.get("role"));
        assertEquals("email", claims.get("provider"));
    }
    
    @Test
    @DisplayName("Should generate valid guest token")
    void shouldGenerateValidGuestToken() throws SeatmapException {
        String sessionId = "guest_123";
        int flightsViewed = 1;
        
        String token = jwtService.generateGuestToken(sessionId, flightsViewed);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Validate the token
        Claims claims = jwtService.validateToken(token);
        assertEquals(sessionId, claims.getSubject());
        assertEquals("guest", claims.get("role"));
        assertEquals("guest", claims.get("provider"));
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> guestLimits = (java.util.Map<String, Object>) claims.get("guestLimits");
        assertEquals(flightsViewed, guestLimits.get("flightsViewed"));
        assertEquals(2, guestLimits.get("maxFlights"));
    }
    
    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserIdFromToken() throws SeatmapException {
        User user = createTestUser();
        String token = jwtService.generateToken(user);
        
        String extractedUserId = jwtService.getUserIdFromToken(token);
        
        assertEquals(user.getUserId(), extractedUserId);
    }
    
    @Test
    @DisplayName("Should extract role from token")
    void shouldExtractRoleFromToken() throws SeatmapException {
        User user = createTestUser();
        String token = jwtService.generateToken(user);
        
        String role = jwtService.getRoleFromToken(token);
        
        assertEquals("user", role);
    }
    
    @Test
    @DisplayName("Should identify guest token")
    void shouldIdentifyGuestToken() throws SeatmapException {
        String guestToken = jwtService.generateGuestToken("guest_123", 0);
        User user = createTestUser();
        String userToken = jwtService.generateToken(user);
        
        assertTrue(jwtService.isGuestToken(guestToken));
        assertFalse(jwtService.isGuestToken(userToken));
    }
    
    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws SeatmapException, InterruptedException {
        User user = createTestUser();
        String originalToken = jwtService.generateToken(user);
        
        // Add a small delay to ensure different timestamps
        Thread.sleep(1000);
        
        String refreshedToken = jwtService.refreshToken(originalToken);
        
        assertNotNull(refreshedToken);
        assertNotEquals(originalToken, refreshedToken);
        
        // Both tokens should have same claims (except timestamps)
        Claims originalClaims = jwtService.validateToken(originalToken);
        Claims refreshedClaims = jwtService.validateToken(refreshedToken);
        
        assertEquals(originalClaims.getSubject(), refreshedClaims.getSubject());
        assertEquals(originalClaims.get("email"), refreshedClaims.get("email"));
        assertEquals(originalClaims.get("role"), refreshedClaims.get("role"));
        assertEquals(originalClaims.get("provider"), refreshedClaims.get("provider"));
    }
    
    @Test
    @DisplayName("Should refresh guest token successfully")
    void shouldRefreshGuestTokenSuccessfully() throws SeatmapException, InterruptedException {
        String originalToken = jwtService.generateGuestToken("guest_123", 1);
        
        // Add a small delay to ensure different timestamps
        Thread.sleep(1000);
        
        String refreshedToken = jwtService.refreshToken(originalToken);
        
        assertNotNull(refreshedToken);
        assertNotEquals(originalToken, refreshedToken);
        
        // Both tokens should have same claims (except timestamps)
        Claims originalClaims = jwtService.validateToken(originalToken);
        Claims refreshedClaims = jwtService.validateToken(refreshedToken);
        
        assertEquals(originalClaims.getSubject(), refreshedClaims.getSubject());
        assertEquals(originalClaims.get("role"), refreshedClaims.get("role"));
        assertEquals(originalClaims.get("provider"), refreshedClaims.get("provider"));
        assertEquals(originalClaims.get("guestLimits"), refreshedClaims.get("guestLimits"));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid token")
    void shouldThrowExceptionForInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(SeatmapException.class, () -> {
            jwtService.validateToken(invalidToken);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for malformed token")
    void shouldThrowExceptionForMalformedToken() {
        String malformedToken = "not-a-jwt-token";
        
        assertThrows(SeatmapException.class, () -> {
            jwtService.validateToken(malformedToken);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for null token")
    void shouldThrowExceptionForNullToken() {
        assertThrows(SeatmapException.class, () -> {
            jwtService.validateToken(null);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for empty token")
    void shouldThrowExceptionForEmptyToken() {
        assertThrows(SeatmapException.class, () -> {
            jwtService.validateToken("");
        });
    }
    
    @Test
    @DisplayName("Should return correct user token expiration time")
    void shouldReturnCorrectUserTokenExpirationTime() {
        int expectedExpiration = 30 * 24 * 60 * 60; // 30 days in seconds
        
        assertEquals(expectedExpiration, jwtService.getTokenExpirationSeconds());
    }

    @Test
    @DisplayName("Should return correct guest token expiration time")
    void shouldReturnCorrectGuestTokenExpirationTime() {
        int expectedExpiration = 24 * 60 * 60; // 24 hours in seconds
        
        assertEquals(expectedExpiration, jwtService.getGuestTokenExpirationSeconds());
    }

    @Test
    @DisplayName("User token should have 30-day expiration")
    void userTokenShouldHave30DayExpiration() throws SeatmapException {
        User user = createTestUser();
        String token = jwtService.generateToken(user);
        
        Claims claims = jwtService.validateToken(token);
        java.util.Date expiration = claims.getExpiration();
        java.util.Date issuedAt = claims.getIssuedAt();
        
        // Calculate difference in seconds
        long diffInSeconds = (expiration.getTime() - issuedAt.getTime()) / 1000;
        int expectedSeconds = 30 * 24 * 60 * 60; // 30 days
        
        // Allow small tolerance for processing time (within 5 seconds)
        assertTrue(Math.abs(diffInSeconds - expectedSeconds) < 5, 
            "User token should expire in approximately 30 days, but was " + diffInSeconds + " seconds");
    }

    @Test
    @DisplayName("Guest token should have 24-hour expiration")
    void guestTokenShouldHave24HourExpiration() throws SeatmapException {
        String sessionId = "guest_123";
        String token = jwtService.generateGuestToken(sessionId, 1);
        
        Claims claims = jwtService.validateToken(token);
        java.util.Date expiration = claims.getExpiration();
        java.util.Date issuedAt = claims.getIssuedAt();
        
        // Calculate difference in seconds
        long diffInSeconds = (expiration.getTime() - issuedAt.getTime()) / 1000;
        int expectedSeconds = 24 * 60 * 60; // 24 hours
        
        // Allow small tolerance for processing time (within 5 seconds)
        assertTrue(Math.abs(diffInSeconds - expectedSeconds) < 5, 
            "Guest token should expire in approximately 24 hours, but was " + diffInSeconds + " seconds");
    }

    @Test
    @DisplayName("Different token types should have different expiration times")
    void differentTokenTypesShouldHaveDifferentExpirationTimes() throws SeatmapException {
        User user = createTestUser();
        String userToken = jwtService.generateToken(user);
        String guestToken = jwtService.generateGuestToken("guest_123", 1);
        
        Claims userClaims = jwtService.validateToken(userToken);
        Claims guestClaims = jwtService.validateToken(guestToken);
        
        java.util.Date userExpiration = userClaims.getExpiration();
        java.util.Date guestExpiration = guestClaims.getExpiration();
        java.util.Date userIssuedAt = userClaims.getIssuedAt();
        java.util.Date guestIssuedAt = guestClaims.getIssuedAt();
        
        long userDiffSeconds = (userExpiration.getTime() - userIssuedAt.getTime()) / 1000;
        long guestDiffSeconds = (guestExpiration.getTime() - guestIssuedAt.getTime()) / 1000;
        
        // User token should be longer than guest token
        assertTrue(userDiffSeconds > guestDiffSeconds, 
            "User token (" + userDiffSeconds + "s) should have longer expiration than guest token (" + guestDiffSeconds + "s)");
        
        // Verify approximate durations
        assertEquals(30 * 24 * 60 * 60, userDiffSeconds, 5);   // 30 days ±5 seconds
        assertEquals(24 * 60 * 60, guestDiffSeconds, 5);        // 24 hours ±5 seconds
    }

    @Test
    @DisplayName("Refreshed user token should maintain user expiration time")
    void refreshedUserTokenShouldMaintainUserExpirationTime() throws SeatmapException, InterruptedException {
        User user = createTestUser();
        String originalToken = jwtService.generateToken(user);
        
        // Add delay to ensure different timestamps
        Thread.sleep(1000);
        
        String refreshedToken = jwtService.refreshToken(originalToken);
        
        Claims refreshedClaims = jwtService.validateToken(refreshedToken);
        java.util.Date expiration = refreshedClaims.getExpiration();
        java.util.Date issuedAt = refreshedClaims.getIssuedAt();
        
        long diffInSeconds = (expiration.getTime() - issuedAt.getTime()) / 1000;
        int expectedUserExpiration = 30 * 24 * 60 * 60; // 30 days
        
        assertTrue(Math.abs(diffInSeconds - expectedUserExpiration) < 5, 
            "Refreshed user token should maintain 30-day expiration");
        assertEquals("user", refreshedClaims.get("role"));
    }

    @Test
    @DisplayName("Refreshed guest token should maintain guest expiration time")
    void refreshedGuestTokenShouldMaintainGuestExpirationTime() throws SeatmapException, InterruptedException {
        String originalToken = jwtService.generateGuestToken("guest_123", 1);
        
        // Add delay to ensure different timestamps
        Thread.sleep(1000);
        
        String refreshedToken = jwtService.refreshToken(originalToken);
        
        Claims refreshedClaims = jwtService.validateToken(refreshedToken);
        java.util.Date expiration = refreshedClaims.getExpiration();
        java.util.Date issuedAt = refreshedClaims.getIssuedAt();
        
        long diffInSeconds = (expiration.getTime() - issuedAt.getTime()) / 1000;
        int expectedGuestExpiration = 24 * 60 * 60; // 24 hours
        
        assertTrue(Math.abs(diffInSeconds - expectedGuestExpiration) < 5, 
            "Refreshed guest token should maintain 24-hour expiration");
        assertEquals("guest", refreshedClaims.get("role"));
    }
    
    @Test
    @DisplayName("Should handle different auth providers")
    void shouldHandleDifferentAuthProviders() throws SeatmapException {
        // Test Google OAuth user
        User googleUser = createTestUser();
        googleUser.setAuthProvider(User.AuthProvider.GOOGLE);
        googleUser.setOauthId("google_oauth_id");
        
        String googleToken = jwtService.generateToken(googleUser);
        Claims googleClaims = jwtService.validateToken(googleToken);
        assertEquals("google", googleClaims.get("provider"));
        
        // Test Apple user
        User appleUser = createTestUser();
        appleUser.setAuthProvider(User.AuthProvider.APPLE);
        appleUser.setOauthId("apple_oauth_id");
        
        String appleToken = jwtService.generateToken(appleUser);
        Claims appleClaims = jwtService.validateToken(appleToken);
        assertEquals("apple", appleClaims.get("provider"));
    }
    
    private User createTestUser() {
        User user = new User();
        user.setUserId("test-user-id");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAuthProvider(User.AuthProvider.EMAIL);
        return user;
    }
}