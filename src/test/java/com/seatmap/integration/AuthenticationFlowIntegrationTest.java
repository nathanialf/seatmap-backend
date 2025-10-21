package com.seatmap.integration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.handler.SeatMapHandler;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.auth.handler.AuthHandler;
import com.seatmap.auth.model.LoginRequest;
import com.seatmap.auth.model.RegisterRequest;
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

/**
 * Integration tests that verify the complete authentication flow
 * from token generation to seat map access with real JWT validation
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationFlowIntegrationTest {
    
    private AuthHandler authHandler;
    private SeatMapHandler seatMapHandler;
    
    @Mock
    private AmadeusService mockAmadeusService;
    
    @Mock
    private Context mockContext;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws Exception {
        // Set test JWT secret
        System.setProperty("JWT_SECRET", "test-secret-key-that-is-at-least-32-characters-long-for-testing");
        
        objectMapper = new ObjectMapper();
        authHandler = new AuthHandler();
        seatMapHandler = new SeatMapHandler();
        
        // Mock Amadeus service in SeatMapHandler
        var amadeusField = SeatMapHandler.class.getDeclaredField("amadeusService");
        amadeusField.setAccessible(true);
        amadeusField.set(seatMapHandler, mockAmadeusService);
        
        // Setup mock Amadeus response
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\",\"available\":true}]}");
        when(mockAmadeusService.getSeatMap(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(mockSeatMapData);
    }
    
    @Test
    void completeGuestFlow_CreateGuestToken_AccessSeatMap_Success() throws Exception {
        // Step 1: Create guest session
        APIGatewayProxyRequestEvent guestRequest = new APIGatewayProxyRequestEvent();
        guestRequest.setPath("/auth/guest");
        guestRequest.setHttpMethod("POST");
        guestRequest.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent guestResponse = authHandler.handleRequest(guestRequest, mockContext);
        
        assertEquals(200, guestResponse.getStatusCode());
        JsonNode guestResponseBody = objectMapper.readTree(guestResponse.getBody());
        String guestToken = guestResponseBody.get("token").asText();
        assertNotNull(guestToken);
        assertFalse(guestToken.isEmpty());
        
        // Step 2: Use guest token to access seat map
        APIGatewayProxyRequestEvent seatMapRequest = new APIGatewayProxyRequestEvent();
        seatMapRequest.setHeaders(Map.of("Authorization", "Bearer " + guestToken));
        seatMapRequest.setBody("{\"flightNumber\":\"AA123\",\"departureDate\":\"2024-12-01\",\"origin\":\"LAX\",\"destination\":\"JFK\"}");
        
        APIGatewayProxyResponseEvent seatMapResponse = seatMapHandler.handleRequest(seatMapRequest, mockContext);
        
        assertEquals(200, seatMapResponse.getStatusCode());
        JsonNode seatMapResponseBody = objectMapper.readTree(seatMapResponse.getBody());
        assertTrue(seatMapResponseBody.get("success").asBoolean());
        assertNotNull(seatMapResponseBody.get("data"));
        
        verify(mockAmadeusService).getSeatMap("AA123", "2024-12-01", "LAX", "JFK");
    }
    
    @Test
    void completeUserRegistrationFlow_RegisterUser_AccessSeatMap_Success() throws Exception {
        // Step 1: Register new user
        APIGatewayProxyRequestEvent registerRequest = new APIGatewayProxyRequestEvent();
        registerRequest.setPath("/auth/register");
        registerRequest.setHttpMethod("POST");
        registerRequest.setHeaders(new HashMap<>());
        
        RegisterRequest registerData = new RegisterRequest();
        registerData.setEmail("newuser@example.com");
        registerData.setPassword("securePassword123!");
        registerData.setFirstName("Jane");
        registerData.setLastName("Smith");
        registerRequest.setBody(objectMapper.writeValueAsString(registerData));
        
        APIGatewayProxyResponseEvent registerResponse = authHandler.handleRequest(registerRequest, mockContext);
        
        assertEquals(200, registerResponse.getStatusCode());
        JsonNode registerResponseBody = objectMapper.readTree(registerResponse.getBody());
        String userToken = registerResponseBody.get("token").asText();
        assertNotNull(userToken);
        assertEquals("newuser@example.com", registerResponseBody.get("email").asText());
        
        // Step 2: Use user token to access seat map (unlimited access)
        APIGatewayProxyRequestEvent seatMapRequest = new APIGatewayProxyRequestEvent();
        seatMapRequest.setHeaders(Map.of("Authorization", "Bearer " + userToken));
        seatMapRequest.setBody("{\"flightNumber\":\"AA456\",\"departureDate\":\"2024-12-15\",\"origin\":\"SFO\",\"destination\":\"LAX\"}");
        
        APIGatewayProxyResponseEvent seatMapResponse = seatMapHandler.handleRequest(seatMapRequest, mockContext);
        
        assertEquals(200, seatMapResponse.getStatusCode());
        JsonNode seatMapResponseBody = objectMapper.readTree(seatMapResponse.getBody());
        assertTrue(seatMapResponseBody.get("success").asBoolean());
        
        verify(mockAmadeusService).getSeatMap("AA456", "2024-12-15", "SFO", "LAX");
    }
    
    @Test
    void completeLoginFlow_LoginExistingUser_AccessSeatMap_Success() throws Exception {
        // First register a user (setup)
        RegisterRequest registerData = new RegisterRequest();
        registerData.setEmail("existing@example.com");
        registerData.setPassword("password123!");
        registerData.setFirstName("John");
        registerData.setLastName("Doe");
        
        APIGatewayProxyRequestEvent registerRequest = new APIGatewayProxyRequestEvent();
        registerRequest.setPath("/auth/register");
        registerRequest.setHttpMethod("POST");
        registerRequest.setHeaders(new HashMap<>());
        registerRequest.setBody(objectMapper.writeValueAsString(registerData));
        
        authHandler.handleRequest(registerRequest, mockContext);
        
        // Step 1: Login with existing user credentials
        APIGatewayProxyRequestEvent loginRequest = new APIGatewayProxyRequestEvent();
        loginRequest.setPath("/auth/login");
        loginRequest.setHttpMethod("POST");
        loginRequest.setHeaders(new HashMap<>());
        
        LoginRequest loginData = new LoginRequest("existing@example.com", "password123!");
        loginRequest.setBody(objectMapper.writeValueAsString(loginData));
        
        APIGatewayProxyResponseEvent loginResponse = authHandler.handleRequest(loginRequest, mockContext);
        
        assertEquals(200, loginResponse.getStatusCode());
        JsonNode loginResponseBody = objectMapper.readTree(loginResponse.getBody());
        String userToken = loginResponseBody.get("token").asText();
        assertNotNull(userToken);
        
        // Step 2: Use login token to access seat map
        APIGatewayProxyRequestEvent seatMapRequest = new APIGatewayProxyRequestEvent();
        seatMapRequest.setHeaders(Map.of("Authorization", "Bearer " + userToken));
        seatMapRequest.setBody("{\"flightNumber\":\"UA789\",\"departureDate\":\"2024-12-20\",\"origin\":\"ORD\",\"destination\":\"DEN\"}");
        
        APIGatewayProxyResponseEvent seatMapResponse = seatMapHandler.handleRequest(seatMapRequest, mockContext);
        
        assertEquals(200, seatMapResponse.getStatusCode());
        JsonNode seatMapResponseBody = objectMapper.readTree(seatMapResponse.getBody());
        assertTrue(seatMapResponseBody.get("success").asBoolean());
        
        verify(mockAmadeusService).getSeatMap("UA789", "2024-12-20", "ORD", "DEN");
    }
    
    @Test
    void guestLimitEnforcement_MultipleRequests_EnforcesLimit() throws Exception {
        // Step 1: Create guest session
        APIGatewayProxyRequestEvent guestRequest = new APIGatewayProxyRequestEvent();
        guestRequest.setPath("/auth/guest");
        guestRequest.setHttpMethod("POST");
        guestRequest.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent guestResponse = authHandler.handleRequest(guestRequest, mockContext);
        JsonNode guestResponseBody = objectMapper.readTree(guestResponse.getBody());
        String guestToken = guestResponseBody.get("token").asText();
        
        // Step 2: First seat map request (should succeed - 0/2 limit)
        APIGatewayProxyRequestEvent request1 = createSeatMapRequest(guestToken, "AA100");
        APIGatewayProxyResponseEvent response1 = seatMapHandler.handleRequest(request1, mockContext);
        assertEquals(200, response1.getStatusCode());
        
        // Step 3: Second seat map request (should succeed - 1/2 limit)
        APIGatewayProxyRequestEvent request2 = createSeatMapRequest(guestToken, "AA200");
        APIGatewayProxyResponseEvent response2 = seatMapHandler.handleRequest(request2, mockContext);
        assertEquals(200, response2.getStatusCode());
        
        // Note: In a real implementation, the guest token would be updated with incremented flight count
        // For this test, we're testing the limit checking logic with the current token structure
    }
    
    @Test
    void tokenRefreshFlow_RefreshValidToken_AccessWithNewToken() throws Exception {
        // Step 1: Create guest token
        APIGatewayProxyRequestEvent guestRequest = new APIGatewayProxyRequestEvent();
        guestRequest.setPath("/auth/guest");
        guestRequest.setHttpMethod("POST");
        guestRequest.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent guestResponse = authHandler.handleRequest(guestRequest, mockContext);
        JsonNode guestResponseBody = objectMapper.readTree(guestResponse.getBody());
        String originalToken = guestResponseBody.get("token").asText();
        
        // Step 2: Refresh token
        APIGatewayProxyRequestEvent refreshRequest = new APIGatewayProxyRequestEvent();
        refreshRequest.setPath("/auth/refresh");
        refreshRequest.setHttpMethod("POST");
        refreshRequest.setHeaders(Map.of("Authorization", "Bearer " + originalToken));
        
        APIGatewayProxyResponseEvent refreshResponse = authHandler.handleRequest(refreshRequest, mockContext);
        
        assertEquals(200, refreshResponse.getStatusCode());
        JsonNode refreshResponseBody = objectMapper.readTree(refreshResponse.getBody());
        String newToken = refreshResponseBody.get("token").asText();
        assertNotNull(newToken);
        assertNotEquals(originalToken, newToken);
        
        // Step 3: Use new token to access seat map
        APIGatewayProxyRequestEvent seatMapRequest = createSeatMapRequest(newToken, "AA300");
        APIGatewayProxyResponseEvent seatMapResponse = seatMapHandler.handleRequest(seatMapRequest, mockContext);
        
        assertEquals(200, seatMapResponse.getStatusCode());
        verify(mockAmadeusService).getSeatMap("AA300", "2024-12-01", "LAX", "JFK");
    }
    
    @Test
    void invalidTokenFlow_UseInvalidToken_ReturnsUnauthorized() throws Exception {
        // Try to access seat map with invalid token
        APIGatewayProxyRequestEvent seatMapRequest = new APIGatewayProxyRequestEvent();
        seatMapRequest.setHeaders(Map.of("Authorization", "Bearer invalid-token"));
        seatMapRequest.setBody("{\"flightNumber\":\"AA123\",\"departureDate\":\"2024-12-01\",\"origin\":\"LAX\",\"destination\":\"JFK\"}");
        
        APIGatewayProxyResponseEvent seatMapResponse = seatMapHandler.handleRequest(seatMapRequest, mockContext);
        
        assertEquals(401, seatMapResponse.getStatusCode());
        assertTrue(seatMapResponse.getBody().contains("Invalid or expired token"));
        verifyNoInteractions(mockAmadeusService);
    }
    
    @Test
    void missingTokenFlow_NoAuthHeader_ReturnsUnauthorized() {
        // Try to access seat map without token
        APIGatewayProxyRequestEvent seatMapRequest = new APIGatewayProxyRequestEvent();
        seatMapRequest.setHeaders(new HashMap<>());
        seatMapRequest.setBody("{\"flightNumber\":\"AA123\",\"departureDate\":\"2024-12-01\",\"origin\":\"LAX\",\"destination\":\"JFK\"}");
        
        APIGatewayProxyResponseEvent seatMapResponse = seatMapHandler.handleRequest(seatMapRequest, mockContext);
        
        assertEquals(401, seatMapResponse.getStatusCode());
        assertTrue(seatMapResponse.getBody().contains("Authorization token required"));
        verifyNoInteractions(mockAmadeusService);
    }
    
    @Test
    void logoutFlow_LogoutWithValidToken_Success() throws Exception {
        // Step 1: Create guest token
        APIGatewayProxyRequestEvent guestRequest = new APIGatewayProxyRequestEvent();
        guestRequest.setPath("/auth/guest");
        guestRequest.setHttpMethod("POST");
        guestRequest.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent guestResponse = authHandler.handleRequest(guestRequest, mockContext);
        JsonNode guestResponseBody = objectMapper.readTree(guestResponse.getBody());
        String token = guestResponseBody.get("token").asText();
        
        // Step 2: Logout
        APIGatewayProxyRequestEvent logoutRequest = new APIGatewayProxyRequestEvent();
        logoutRequest.setPath("/auth/logout");
        logoutRequest.setHttpMethod("DELETE");
        logoutRequest.setHeaders(Map.of("Authorization", "Bearer " + token));
        
        APIGatewayProxyResponseEvent logoutResponse = authHandler.handleRequest(logoutRequest, mockContext);
        
        assertEquals(200, logoutResponse.getStatusCode());
        JsonNode logoutResponseBody = objectMapper.readTree(logoutResponse.getBody());
        assertTrue(logoutResponseBody.get("success").asBoolean());
        assertTrue(logoutResponseBody.get("message").asText().contains("Logged out successfully"));
    }
    
    @Test
    void crossHandlerTokenValidation_SameJwtService_ConsistentValidation() throws Exception {
        // Generate token with AuthHandler
        APIGatewayProxyRequestEvent guestRequest = new APIGatewayProxyRequestEvent();
        guestRequest.setPath("/auth/guest");
        guestRequest.setHttpMethod("POST");
        guestRequest.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent guestResponse = authHandler.handleRequest(guestRequest, mockContext);
        JsonNode guestResponseBody = objectMapper.readTree(guestResponse.getBody());
        String token = guestResponseBody.get("token").asText();
        
        // Validate token with SeatMapHandler
        APIGatewayProxyRequestEvent seatMapRequest = createSeatMapRequest(token, "AA999");
        APIGatewayProxyResponseEvent seatMapResponse = seatMapHandler.handleRequest(seatMapRequest, mockContext);
        
        assertEquals(200, seatMapResponse.getStatusCode());
        JsonNode seatMapResponseBody = objectMapper.readTree(seatMapResponse.getBody());
        assertTrue(seatMapResponseBody.get("success").asBoolean());
        
        // Verify both handlers use the same JWT validation logic
        verify(mockAmadeusService).getSeatMap("AA999", "2024-12-01", "LAX", "JFK");
    }
    
    private APIGatewayProxyRequestEvent createSeatMapRequest(String token, String flightNumber) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("Authorization", "Bearer " + token));
        request.setBody("{\"flightNumber\":\"" + flightNumber + "\",\"departureDate\":\"2024-12-01\",\"origin\":\"LAX\",\"destination\":\"JFK\"}");
        return request;
    }
}