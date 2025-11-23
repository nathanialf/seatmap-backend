package com.seatmap.auth.service;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import io.jsonwebtoken.Claims;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive security and reliability tests for JwtService.
 * These tests cover critical scenarios that could cause security issues or system failures.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Security Tests")
class JwtServiceSecurityTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Test
    @DisplayName("User token should contain all required claims and be valid")
    void userToken_ShouldContainRequiredClaimsAndBeValid() throws Exception {
        // Given
        User user = createTestUser("user123", "test@example.com", User.AuthProvider.EMAIL);

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."), "JWT should have dot separators");
        assertEquals(3, token.split("\\.").length, "JWT should have 3 parts");

        // Validate token contents
        Claims claims = jwtService.validateToken(token);
        assertEquals("user123", claims.getSubject());
        assertEquals("test@example.com", claims.get("email"));
        assertEquals("user", claims.get("role"));
        assertEquals("email", claims.get("provider"));
        assertNotNull(claims.get("iat"));
        assertNotNull(claims.get("exp"));

        // Should not be guest token
        assertFalse(jwtService.isGuestToken(token));
    }

    @Test
    @DisplayName("Guest token should contain guest-specific claims and limits")
    void guestToken_ShouldContainGuestSpecificClaimsAndLimits() throws Exception {
        // Given
        String guestId = "guest_abc123";
        int flightsViewed = 1;

        // When
        String token = jwtService.generateGuestToken(guestId, flightsViewed);

        // Then
        assertNotNull(token);
        assertTrue(token.contains("."));
        assertEquals(3, token.split("\\.").length);

        // Validate token contents
        Map<String, Object> claims = jwtService.validateToken(token);
        assertEquals(guestId, claims.get("sub"));
        assertEquals("guest", claims.get("role"));
        assertEquals("guest", claims.get("provider"));
        assertNotNull(claims.get("guestLimits"));

        // Should be guest token
        assertTrue(jwtService.isGuestToken(token));

        // Validate guest limits structure
        @SuppressWarnings("unchecked")
        Map<String, Object> guestLimits = (Map<String, Object>) claims.get("guestLimits");
        assertEquals(flightsViewed, guestLimits.get("flightsViewed"));
        assertEquals(2, guestLimits.get("maxFlights"));
    }

    @Test
    @DisplayName("Token validation should reject various malformed inputs")
    void tokenValidation_ShouldRejectMalformedInputs() {
        // Test various malformed token formats that could cause security issues
        String[] malformedTokens = {
            null,
            "",
            "   ",
            "invalid",
            "not.a.jwt",
            "header.payload", // Missing signature
            "too.many.parts.here.invalid",
            "eyJhbGciOiJIUzI1NiJ9.invalid_base64.signature", // Invalid base64 payload
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.invalid_signature" // Invalid signature
        };

        for (String malformedToken : malformedTokens) {
            assertThrows(SeatmapException.class, () -> {
                jwtService.validateToken(malformedToken);
            }, "Should reject malformed token: " + malformedToken);
        }
    }

    @Test
    @DisplayName("Token refresh should preserve core claims but update timestamps")
    void tokenRefresh_ShouldPreserveCoreClaimsButUpdateTimestamps() throws Exception {
        // Given
        User user = createTestUser("user123", "test@example.com", User.AuthProvider.EMAIL);
        String originalToken = jwtService.generateToken(user);
        Claims originalClaims = jwtService.validateToken(originalToken);

        // When (add small delay to ensure different timestamp)
        Thread.sleep(1000);
        String refreshedToken = jwtService.refreshToken(originalToken);

        // Then
        assertNotNull(refreshedToken);
        assertNotEquals(originalToken, refreshedToken, "Refreshed token should be different");

        Claims refreshedClaims = jwtService.validateToken(refreshedToken);
        
        // Core claims should be preserved
        assertEquals(originalClaims.getSubject(), refreshedClaims.getSubject());
        assertEquals(originalClaims.get("role"), refreshedClaims.get("role"));
        assertEquals(originalClaims.get("provider"), refreshedClaims.get("provider"));
        assertEquals(originalClaims.get("email"), refreshedClaims.get("email"));

        // Timestamps should be different (new token)
        assertNotEquals(originalClaims.getIssuedAt(), refreshedClaims.getIssuedAt());
        assertNotEquals(originalClaims.getExpiration(), refreshedClaims.getExpiration());
    }

    @Test
    @DisplayName("Guest token refresh should preserve guest limits")
    void guestTokenRefresh_ShouldPreserveGuestLimits() throws Exception {
        // Given
        String originalGuestToken = jwtService.generateGuestToken("guest_123", 1);
        
        // When
        String refreshedToken = jwtService.refreshToken(originalGuestToken);
        
        // Then
        Map<String, Object> refreshedClaims = jwtService.validateToken(refreshedToken);
        assertTrue(jwtService.isGuestToken(refreshedToken));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> guestLimits = (Map<String, Object>) refreshedClaims.get("guestLimits");
        assertEquals(1, guestLimits.get("flightsViewed"));
        assertEquals(2, guestLimits.get("maxFlights"));
    }

    @Test
    @DisplayName("User ID extraction should work correctly for all token types")
    void userIdExtraction_ShouldWorkCorrectlyForAllTokenTypes() throws Exception {
        // Test user token
        User user = createTestUser("user123", "test@example.com", User.AuthProvider.EMAIL);
        String userToken = jwtService.generateToken(user);
        String extractedUserId = jwtService.getUserIdFromToken(userToken);
        assertEquals("user123", extractedUserId);

        // Test guest token
        String guestToken = jwtService.generateGuestToken("guest_456", 0);
        String extractedGuestId = jwtService.getUserIdFromToken(guestToken);
        assertEquals("guest_456", extractedGuestId);
    }

    @Test
    @DisplayName("Token validation should be thread-safe under concurrent access")
    void tokenValidation_ShouldBeThreadSafeUnderConcurrentAccess() throws Exception {
        // Generate test tokens
        User user = createTestUser("user123", "test@example.com", User.AuthProvider.EMAIL);
        String userToken = jwtService.generateToken(user);
        String guestToken = jwtService.generateGuestToken("guest_456", 1);

        // Test concurrent validation
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    // Alternate between validating user and guest tokens
                    String tokenToValidate = threadIndex % 2 == 0 ? userToken : guestToken;
                    Map<String, Object> claims = jwtService.validateToken(tokenToValidate);
                    
                    assertNotNull(claims);
                    assertNotNull(claims.get("sub"));
                    assertNotNull(claims.get("role"));
                    
                    // Validate role matches token type
                    if (threadIndex % 2 == 0) {
                        assertEquals("user", claims.get("role"));
                        assertFalse(jwtService.isGuestToken(tokenToValidate));
                    } else {
                        assertEquals("guest", claims.get("role"));
                        assertTrue(jwtService.isGuestToken(tokenToValidate));
                    }
                } catch (Exception e) {
                    fail("Token validation should not fail in concurrent environment: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");

        // Ensure all futures completed successfully
        CompletableFuture.allOf(futures).get(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Token expiration configuration should be reasonable")
    void tokenExpiration_ShouldBeReasonable() {
        // Verify token expiration is configured properly
        int expirationSeconds = jwtService.getTokenExpirationSeconds();
        assertTrue(expirationSeconds > 0, "Token should have positive expiration time");
        assertTrue(expirationSeconds <= 2592000, "Token expiration should be reasonable (≤30 days)");
        assertTrue(expirationSeconds >= 3600, "Token expiration should be at least 1 hour for usability");
    }

    @Test
    @DisplayName("Guest token should handle edge cases in flight counts")
    void guestToken_ShouldHandleEdgeCasesInFlightCounts() throws Exception {
        // Test zero flights viewed
        String token0 = jwtService.generateGuestToken("guest_0", 0);
        Map<String, Object> claims0 = jwtService.validateToken(token0);
        @SuppressWarnings("unchecked")
        Map<String, Object> limits0 = (Map<String, Object>) claims0.get("guestLimits");
        assertEquals(0, limits0.get("flightsViewed"));

        // Test maximum flights viewed
        String token2 = jwtService.generateGuestToken("guest_max", 2);
        Map<String, Object> claims2 = jwtService.validateToken(token2);
        @SuppressWarnings("unchecked")
        Map<String, Object> limits2 = (Map<String, Object>) claims2.get("guestLimits");
        assertEquals(2, limits2.get("flightsViewed"));

        // Test negative flights viewed (should be preserved as-is for debugging)
        String tokenNeg = jwtService.generateGuestToken("guest_neg", -1);
        Map<String, Object> claimsNeg = jwtService.validateToken(tokenNeg);
        @SuppressWarnings("unchecked")
        Map<String, Object> limitsNeg = (Map<String, Object>) claimsNeg.get("guestLimits");
        assertEquals(-1, limitsNeg.get("flightsViewed"));
    }

    @Test
    @DisplayName("Role identification should be accurate and consistent")
    void roleIdentification_ShouldBeAccurateAndConsistent() throws Exception {
        // Test user token
        User user = createTestUser("user123", "test@example.com", User.AuthProvider.EMAIL);
        String userToken = jwtService.generateToken(user);
        assertFalse(jwtService.isGuestToken(userToken));

        // Test guest token
        String guestToken = jwtService.generateGuestToken("guest_123", 1);
        assertTrue(jwtService.isGuestToken(guestToken));

        // Test that role determination is consistent with validation
        Map<String, Object> userClaims = jwtService.validateToken(userToken);
        assertEquals("user", userClaims.get("role"));

        Map<String, Object> guestClaims = jwtService.validateToken(guestToken);
        assertEquals("guest", guestClaims.get("role"));
    }

    @Test
    @DisplayName("Token validation should handle input sanitization properly")
    void tokenValidation_ShouldHandleInputSanitizationProperly() {
        // Test tokens with various problematic inputs that could cause issues
        String[] problematicInputs = {
            "token with spaces",
            "token\nwith\nnewlines",
            "token\twith\ttabs",
            "token@#$%^&*()",
            "very.long.token.that.goes.on.and.on.and.on.and.on.and.on.and.on",
            "\0null\0characters\0",
            "../../etc/passwd", // Path traversal attempt
            "<script>alert('xss')</script>", // XSS attempt
            "'; DROP TABLE tokens; --" // SQL injection attempt
        };

        for (String input : problematicInputs) {
            assertThrows(SeatmapException.class, () -> {
                jwtService.validateToken(input);
            }, "Should safely reject problematic input: " + input);
        }
    }

    @Test
    @DisplayName("Different auth providers should be handled correctly")
    void differentAuthProviders_ShouldBeHandledCorrectly() throws Exception {
        // Test all auth provider types
        for (User.AuthProvider provider : User.AuthProvider.values()) {
            User user = createTestUser("user_" + provider.name(), "test@example.com", provider);
            String token = jwtService.generateToken(user);
            
            Map<String, Object> claims = jwtService.validateToken(token);
            assertEquals(provider.name().toLowerCase(), claims.get("provider"));
            assertEquals("user", claims.get("role"));
            assertFalse(jwtService.isGuestToken(token));
        }
    }

    // Helper method to create test users
    private User createTestUser(String userId, String email, User.AuthProvider authProvider) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(email);
        user.setAuthProvider(authProvider);
        return user;
    }
}