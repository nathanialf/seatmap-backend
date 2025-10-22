package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapException;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.auth.service.JwtService;
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
class SeatMapHandlerTest {
    
    private SeatMapHandler handler;
    
    @Mock
    private AmadeusService mockAmadeusService;
    
    @Mock
    private JwtService mockJwtService;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private Claims mockClaims;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new SeatMapHandler();
        
        // Use reflection to inject mocks
        try {
            var amadeusField = SeatMapHandler.class.getDeclaredField("amadeusService");
            amadeusField.setAccessible(true);
            amadeusField.set(handler, mockAmadeusService);
            
            var jwtField = SeatMapHandler.class.getDeclaredField("jwtService");
            jwtField.setAccessible(true);
            jwtField.set(handler, mockJwtService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }
    
    @Test
    void handleRequest_WithValidUserToken_ReturnsSuccess() throws Exception {
        // Setup request with flight offer data
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        String flightOfferData = "{\"id\":\"offer123\",\"type\":\"flight-offer\",\"source\":\"GDS\",\"itineraries\":[{\"segments\":[{\"departure\":{\"iataCode\":\"LAX\"},\"arrival\":{\"iataCode\":\"JFK\"},\"carrierCode\":\"AA\",\"number\":\"123\"}]}]}";
        request.setBody("{\"flightOfferId\":\"offer123\",\"flightOfferData\":\"" + flightOfferData.replace("\"", "\\\"") + "\"}");
        
        // Mock JWT validation
        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("valid-token")).thenReturn(false);
        
        // Mock Amadeus response
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenReturn(mockSeatMapData);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        verify(mockAmadeusService).getSeatMapFromOfferData(anyString());
    }
    
    @Test
    void handleRequest_WithMissingAuthHeader_ReturnsUnauthorized() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
    }
    
    @Test
    void handleRequest_WithInvalidAuthHeader_ReturnsUnauthorized() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("Authorization", "Invalid token-format"));
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
    }
    
    @Test
    void handleRequest_WithInvalidToken_ReturnsUnauthorized() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("Authorization", "Bearer invalid-token"));
        
        when(mockJwtService.validateToken("invalid-token"))
            .thenThrow(com.seatmap.common.exception.SeatmapException.unauthorized("Invalid token"));
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }
    
    // NOTE: Guest token limiting is now handled by IP-based approach in SeatMapHandlerGuestLimitTest
    // This test was removed since the new implementation uses IP-based guest limiting via GuestAccessRepository
    // rather than token-based claims
    
    @Test
    void handleRequest_WithInvalidRequestBody_ReturnsBadRequest() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        request.setBody("invalid-json");
        
        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("valid-token")).thenReturn(false);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
    }
    
    @Test
    void handleRequest_WithValidationErrors_ReturnsBadRequest() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        request.setBody("{\"flightOfferId\":\"\",\"flightOfferData\":\"\"}"); // Missing required fields
        
        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("valid-token")).thenReturn(false);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Validation errors"));
    }
    
    @Test
    void handleRequest_WithAmadeusServiceError_ReturnsInternalServerError() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        String flightOfferData = "{\"id\":\"offer123\",\"type\":\"flight-offer\"}";
        request.setBody("{\"flightOfferId\":\"offer123\",\"flightOfferData\":\"" + flightOfferData.replace("\"", "\\\"") + "\"}");
        
        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("valid-token")).thenReturn(false);
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenThrow(new SeatmapException("Amadeus API error"));
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Amadeus API error"));
    }
    
    @Test
    void handleRequest_ResponseIncludesCorsHeaders() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        Map<String, String> headers = response.getHeaders();
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, POST, PUT, DELETE, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, Authorization", headers.get("Access-Control-Allow-Headers"));
        assertEquals("application/json", headers.get("Content-Type"));
    }
}