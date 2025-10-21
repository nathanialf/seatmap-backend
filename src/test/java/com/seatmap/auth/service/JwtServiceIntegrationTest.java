package com.seatmap.auth.service;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceIntegrationTest {
    
    private JwtService jwtService;
    
    @BeforeEach
    void setUp() {
        // Set test JWT secret in environment variable
        System.setProperty("JWT_SECRET", "test-secret-key-that-is-at-least-32-characters-long-for-testing");
        jwtService = new JwtService();
    }
    
    @Test
    void generateAndValidateUserToken_Success() throws Exception {
        // Create test user
        User user = createTestUser();
        
        // Generate token
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Validate token
        Claims claims = jwtService.validateToken(token);
        
        assertEquals(user.getUserId(), claims.getSubject());
        assertEquals(user.getEmail(), claims.get("email"));
        assertEquals("user", claims.get("role"));
        assertEquals("email", claims.get("provider"));
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(new Date()));
    }
    
    @Test
    void generateAndValidateGuestToken_Success() throws Exception {
        // Generate guest token
        String guestId = "guest_123";
        int flightsViewed = 0;
        String token = jwtService.generateGuestToken(guestId, flightsViewed);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Validate token
        Claims claims = jwtService.validateToken(token);
        
        assertEquals(guestId, claims.getSubject());
        assertEquals("guest", claims.get("role"));
        assertEquals("guest", claims.get("provider"));
        
        // Verify guest limits
        Object guestLimitsObj = claims.get("guestLimits");
        assertNotNull(guestLimitsObj);
        assertTrue(guestLimitsObj instanceof java.util.Map);
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> guestLimits = (java.util.Map<String, Object>) guestLimitsObj;
        assertEquals(0, guestLimits.get("flightsViewed"));
        assertEquals(2, guestLimits.get("maxFlights"));
    }
    
    @Test
    void isGuestToken_WithUserToken_ReturnsFalse() throws Exception {
        User user = createTestUser();
        String token = jwtService.generateToken(user);
        
        assertFalse(jwtService.isGuestToken(token));
    }
    
    @Test
    void isGuestToken_WithGuestToken_ReturnsTrue() throws Exception {
        String token = jwtService.generateGuestToken("guest_123", 0);
        
        assertTrue(jwtService.isGuestToken(token));
    }
    
    @Test
    void getUserIdFromToken_WithUserToken_ReturnsUserId() throws Exception {
        User user = createTestUser();
        String token = jwtService.generateToken(user);
        
        String userId = jwtService.getUserIdFromToken(token);
        assertEquals(user.getUserId(), userId);
    }
    
    @Test
    void getUserIdFromToken_WithGuestToken_ReturnsGuestId() throws Exception {
        String guestId = "guest_456";
        String token = jwtService.generateGuestToken(guestId, 1);
        
        String userId = jwtService.getUserIdFromToken(token);
        assertEquals(guestId, userId);
    }
    
    @Test
    void refreshToken_WithValidUserToken_ReturnsNewToken() throws Exception {
        User user = createTestUser();
        String originalToken = jwtService.generateToken(user);
        
        // Wait a small amount to ensure different timestamps
        Thread.sleep(1000);
        
        String refreshedToken = jwtService.refreshToken(originalToken);
        
        assertNotNull(refreshedToken);
        assertNotEquals(originalToken, refreshedToken);
        
        // Validate both tokens have same core claims but different timestamps
        Claims originalClaims = jwtService.validateToken(originalToken);
        Claims refreshedClaims = jwtService.validateToken(refreshedToken);
        
        assertEquals(originalClaims.getSubject(), refreshedClaims.getSubject());
        assertEquals(originalClaims.get("email"), refreshedClaims.get("email"));
        assertEquals(originalClaims.get("role"), refreshedClaims.get("role"));
        assertTrue(refreshedClaims.getIssuedAt().after(originalClaims.getIssuedAt()));
    }
    
    @Test
    void refreshToken_WithValidGuestToken_ReturnsNewToken() throws Exception {
        String guestId = "guest_789";
        String originalToken = jwtService.generateGuestToken(guestId, 1);
        
        Thread.sleep(1000);
        
        String refreshedToken = jwtService.refreshToken(originalToken);
        
        assertNotNull(refreshedToken);
        assertNotEquals(originalToken, refreshedToken);
        
        // Validate guest limits are preserved
        Claims refreshedClaims = jwtService.validateToken(refreshedToken);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> guestLimits = (java.util.Map<String, Object>) refreshedClaims.get("guestLimits");
        assertEquals(1, guestLimits.get("flightsViewed"));
        assertEquals(2, guestLimits.get("maxFlights"));
    }
    
    @Test
    void validateToken_WithExpiredToken_ThrowsException() throws Exception {
        // Create token with past expiration
        User user = createTestUser();
        
        // Use reflection to create an expired token
        JwtService expiredJwtService = createJwtServiceWithCustomExpiration(-3600); // -1 hour
        String expiredToken = expiredJwtService.generateToken(user);
        
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            jwtService.validateToken(expiredToken);
        });
        
        assertTrue(exception.getMessage().contains("Token has expired"));
    }
    
    @Test
    void validateToken_WithMalformedToken_ThrowsException() {
        String malformedToken = "not.a.valid.jwt.token";
        
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            jwtService.validateToken(malformedToken);
        });
        
        assertTrue(exception.getMessage().contains("Invalid token format"));
    }
    
    @Test
    void validateToken_WithInvalidSignature_ThrowsException() throws Exception {
        // Create token with one secret, try to validate with different secret
        User user = createTestUser();
        String token = jwtService.generateToken(user);
        
        // Create new JwtService with different secret
        System.setProperty("JWT_SECRET", "different-secret-key-that-is-at-least-32-characters-long");
        JwtService differentSecretService = new JwtService();
        
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            differentSecretService.validateToken(token);
        });
        
        assertTrue(exception.getMessage().contains("Invalid token signature"));
    }
    
    @Test
    void validateToken_WithNullToken_ThrowsException() {
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            jwtService.validateToken(null);
        });
        
        assertTrue(exception.getMessage().contains("Invalid token"));
    }
    
    @Test
    void validateToken_WithEmptyToken_ThrowsException() {
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            jwtService.validateToken("");
        });
        
        assertTrue(exception.getMessage().contains("Invalid token"));
    }
    
    @Test
    void constructor_WithTooShortSecret_ThrowsException() {
        System.setProperty("JWT_SECRET", "short");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new JwtService();
        });
        
        assertTrue(exception.getMessage().contains("at least 32 characters"));
    }
    
    @Test
    void constructor_WithNullSecret_ThrowsException() {
        System.clearProperty("JWT_SECRET");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new JwtService();
        });
        
        assertTrue(exception.getMessage().contains("must be set"));
    }
    
    @Test
    void getTokenExpirationSeconds_ReturnsCorrectValue() {
        int expirationSeconds = jwtService.getTokenExpirationSeconds();
        assertEquals(86400, expirationSeconds); // 24 hours
    }
    
    @Test
    void generateGuestToken_WithDifferentFlightsViewed_CreatesCorrectClaims() throws Exception {
        String guestId = "guest_test";
        
        // Test with 0 flights viewed
        String token0 = jwtService.generateGuestToken(guestId, 0);
        Claims claims0 = jwtService.validateToken(token0);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> limits0 = (java.util.Map<String, Object>) claims0.get("guestLimits");
        assertEquals(0, limits0.get("flightsViewed"));
        
        // Test with 1 flight viewed
        String token1 = jwtService.generateGuestToken(guestId, 1);
        Claims claims1 = jwtService.validateToken(token1);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> limits1 = (java.util.Map<String, Object>) claims1.get("guestLimits");
        assertEquals(1, limits1.get("flightsViewed"));
        
        // Test with max flights viewed
        String token2 = jwtService.generateGuestToken(guestId, 2);
        Claims claims2 = jwtService.validateToken(token2);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> limits2 = (java.util.Map<String, Object>) claims2.get("guestLimits");
        assertEquals(2, limits2.get("flightsViewed"));
    }
    
    private User createTestUser() {
        User user = new User();
        user.setUserId("user-123");
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setAuthProvider(User.AuthProvider.EMAIL);
        user.setStatus(User.UserStatus.ACTIVE);
        return user;
    }
    
    private JwtService createJwtServiceWithCustomExpiration(int expirationOffsetSeconds) throws Exception {
        JwtService service = new JwtService();
        
        // Use reflection to modify the expiration offset
        Field expirationField = JwtService.class.getDeclaredField("tokenExpirationSeconds");
        expirationField.setAccessible(true);
        expirationField.set(service, expirationOffsetSeconds);
        
        return service;
    }
}