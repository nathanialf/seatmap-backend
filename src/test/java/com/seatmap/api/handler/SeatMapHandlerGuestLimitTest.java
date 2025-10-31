package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.auth.service.UserUsageLimitsService;
import com.seatmap.common.model.User;
import com.seatmap.common.model.User.AccountTier;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatMapHandlerGuestLimitTest {
    
    private SeatMapHandler handler;
    
    @Mock
    private AmadeusService mockAmadeusService;
    
    @Mock
    private JwtService mockJwtService;
    
    @Mock
    private GuestAccessRepository mockGuestAccessRepository;
    
    @Mock
    private AuthService mockAuthService;
    
    @Mock
    private UserUsageLimitsService mockUserUsageLimitsService;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private Claims mockClaims;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Set test environment variables to avoid AmadeusService initialization failure
        System.setProperty("AMADEUS_API_KEY", "test-key");
        System.setProperty("AMADEUS_API_SECRET", "test-secret");
        System.setProperty("AMADEUS_ENDPOINT", "test.amadeus.com");
        
        handler = new SeatMapHandler();
        
        // Use reflection to inject mocks
        try {
            injectMock("amadeusService", mockAmadeusService);
            injectMock("jwtService", mockJwtService);
            injectMock("guestAccessRepository", mockGuestAccessRepository);
            injectMock("authService", mockAuthService);
            injectMock("userUsageLimitsService", mockUserUsageLimitsService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
        
    }
    
    private void injectMock(String fieldName, Object mock) throws Exception {
        var field = SeatMapHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, mock);
    }
    
    private User createTestUser(String userId, AccountTier tier) {
        User user = new User();
        user.setUserId(userId);
        user.setAccountTier(tier);
        user.setEmail("test@example.com");
        return user;
    }
    
    @Test
    void handleRequest_WithGuestToken_CanMakeRequest_ReturnsSuccess() throws Exception {
        // Setup request with IP
        APIGatewayProxyRequestEvent request = createValidSeatMapRequestWithIp("192.168.1.100");
        
        // Mock JWT validation for guest token
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // Mock IP-based limiting - allow request
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        
        // Mock Amadeus response
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenReturn(mockSeatMapData);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        
        // Verify seatmap request was recorded after successful call
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.100");
        verify(mockGuestAccessRepository).recordSeatmapRequest("192.168.1.100");
        verify(mockAmadeusService).getSeatMapFromOfferData(anyString());
    }
    
    @Test
    void handleRequest_WithGuestToken_AtLimit_ReturnsForbidden() throws Exception {
        // Setup request with IP
        APIGatewayProxyRequestEvent request = createValidSeatMapRequestWithIp("192.168.1.101");
        
        // Mock JWT validation for guest token
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // Mock IP-based limiting - deny request
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.101")).thenReturn(false);
        when(mockGuestAccessRepository.getSeatmapDenialMessage("192.168.1.101"))
            .thenReturn("You've used your 2 free seat map views. Please register for unlimited seat map access.");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(403, response.getStatusCode());
        assertTrue(response.getBody().contains("You've used your 2 free seat map views"));
        assertTrue(response.getBody().contains("register"));
        
        // Verify no Amadeus call was made and no seatmap request was recorded
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.101");
        verify(mockGuestAccessRepository).getSeatmapDenialMessage("192.168.1.101");
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest(anyString());
        verifyNoInteractions(mockAmadeusService);
    }
    
    @Test
    void handleRequest_WithGuestToken_IpExtraction_FromXForwardedFor() throws Exception {
        // Setup request with X-Forwarded-For header
        APIGatewayProxyRequestEvent request = createValidSeatMapRequest();
        Map<String, String> headers = new HashMap<>(request.getHeaders());
        headers.put("X-Forwarded-For", "203.0.113.195, 192.168.1.1");
        request.setHeaders(headers);
        
        // Mock JWT validation for guest token
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // Mock IP-based limiting for extracted IP
        when(mockGuestAccessRepository.canMakeSeatmapRequest("203.0.113.195")).thenReturn(true);
        
        // Mock Amadeus response
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenReturn(mockSeatMapData);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        
        // Verify the first IP from X-Forwarded-For was used
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("203.0.113.195");
        verify(mockGuestAccessRepository).recordSeatmapRequest("203.0.113.195");
    }
    
    @Test
    void handleRequest_WithGuestToken_IpExtraction_FromSourceIp() throws Exception {
        // Setup request without forwarded headers, but with source IP
        APIGatewayProxyRequestEvent request = createValidSeatMapRequest();
        
        // Set up source IP fallback
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        APIGatewayProxyRequestEvent.RequestIdentity identity = new APIGatewayProxyRequestEvent.RequestIdentity();
        identity.setSourceIp("172.16.0.10");
        context.setIdentity(identity);
        request.setRequestContext(context);
        
        // Mock JWT validation for guest token
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // Mock IP-based limiting for source IP
        when(mockGuestAccessRepository.canMakeSeatmapRequest("172.16.0.10")).thenReturn(true);
        
        // Mock Amadeus response
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenReturn(mockSeatMapData);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        
        // Verify source IP was used
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("172.16.0.10");
        verify(mockGuestAccessRepository).recordSeatmapRequest("172.16.0.10");
    }
    
    @Test
    void handleRequest_WithGuestToken_UnknownIp_HandlesGracefully() throws Exception {
        // Setup request with no IP sources but keep Authorization header
        APIGatewayProxyRequestEvent request = createValidSeatMapRequest();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer guest-token"); // Keep auth header
        request.setHeaders(headers);
        request.setRequestContext(null);
        
        // Mock JWT validation for guest token
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // Mock IP-based limiting for "unknown"
        when(mockGuestAccessRepository.canMakeSeatmapRequest("unknown")).thenReturn(true);
        
        // Mock Amadeus response
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenReturn(mockSeatMapData);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        
        // Verify "unknown" IP was used
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("unknown");
        verify(mockGuestAccessRepository).recordSeatmapRequest("unknown");
    }
    
    @Test
    void handleRequest_WithUserToken_BypassesGuestLimits() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = createValidSeatMapRequestWithIp("192.168.1.200");
        Map<String, String> headers = new HashMap<>(request.getHeaders());
        headers.put("Authorization", "Bearer user-token");
        request.setHeaders(headers);
        
        // Mock JWT validation for regular user token (not guest)
        when(mockJwtService.validateToken("user-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("user-token")).thenReturn(false);
        
        // Mock user authentication and tier limits
        User testUser = createTestUser("test-user-id", AccountTier.PRO);
        when(mockAuthService.validateToken("user-token")).thenReturn(testUser);
        when(mockUserUsageLimitsService.canMakeSeatmapRequest(testUser)).thenReturn(true);
        
        // Mock Amadeus response
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenReturn(mockSeatMapData);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        
        // Verify that guest access repository was never called for user tokens
        verifyNoInteractions(mockGuestAccessRepository);
        verify(mockAmadeusService).getSeatMapFromOfferData(anyString());
    }
    
    @Test
    void handleRequest_WithGuestToken_GuestAccessException_ReturnsUnauthorized() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = createValidSeatMapRequestWithIp("192.168.1.102");
        
        // Mock JWT validation for guest token
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // Mock guest access repository to throw exception
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.102"))
            .thenThrow(new RuntimeException("Database error"));
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Error validating access limits"));
        
        verifyNoInteractions(mockAmadeusService);
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest(anyString());
    }
    
    @Test
    void handleRequest_WithGuestToken_RecordingFails_ContinuesSuccessfully() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = createValidSeatMapRequestWithIp("192.168.1.103");
        
        // Mock JWT validation for guest token
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // Mock IP-based limiting - allow request
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.103")).thenReturn(true);
        
        // Mock Amadeus response
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenReturn(mockSeatMapData);
        
        // Mock recording to fail (should not fail the request)
        doThrow(new RuntimeException("Recording failed")).when(mockGuestAccessRepository)
            .recordSeatmapRequest("192.168.1.103");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Should still succeed despite recording failure
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.103");
        verify(mockGuestAccessRepository).recordSeatmapRequest("192.168.1.103");
        verify(mockAmadeusService).getSeatMapFromOfferData(anyString());
    }
    
    @Test
    void handleRequest_WithGuestToken_MultipleIpsIndependent() throws Exception {
        // Test that different IPs are tracked independently
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";
        
        // First request from IP1
        APIGatewayProxyRequestEvent request1 = createValidSeatMapRequestWithIp(ip1);
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        when(mockGuestAccessRepository.canMakeSeatmapRequest(ip1)).thenReturn(true);
        
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenReturn(mockSeatMapData);
        
        APIGatewayProxyResponseEvent response1 = handler.handleRequest(request1, mockContext);
        assertEquals(200, response1.getStatusCode());
        
        // Second request from IP2
        APIGatewayProxyRequestEvent request2 = createValidSeatMapRequestWithIp(ip2);
        when(mockGuestAccessRepository.canMakeSeatmapRequest(ip2)).thenReturn(true);
        
        APIGatewayProxyResponseEvent response2 = handler.handleRequest(request2, mockContext);
        assertEquals(200, response2.getStatusCode());
        
        // Verify both IPs were tracked separately
        verify(mockGuestAccessRepository).canMakeSeatmapRequest(ip1);
        verify(mockGuestAccessRepository).recordSeatmapRequest(ip1);
        verify(mockGuestAccessRepository).canMakeSeatmapRequest(ip2);
        verify(mockGuestAccessRepository).recordSeatmapRequest(ip2);
    }
    
    private APIGatewayProxyRequestEvent createValidSeatMapRequest() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("Authorization", "Bearer guest-token"));
        String flightOfferData = "{\"id\":\"offer123\",\"type\":\"flight-offer\",\"source\":\"GDS\",\"itineraries\":[{\"segments\":[{\"departure\":{\"iataCode\":\"LAX\"},\"arrival\":{\"iataCode\":\"JFK\"},\"carrierCode\":\"AA\",\"number\":\"123\"}]}]}";
        request.setBody("{\"flightOfferData\":\"" + flightOfferData.replace("\"", "\\\"") + "\"}");
        return request;
    }
    
    private APIGatewayProxyRequestEvent createValidSeatMapRequestWithIp(String ip) {
        APIGatewayProxyRequestEvent request = createValidSeatMapRequest();
        Map<String, String> headers = new HashMap<>(request.getHeaders());
        headers.put("X-Forwarded-For", ip);
        request.setHeaders(headers);
        return request;
    }
}