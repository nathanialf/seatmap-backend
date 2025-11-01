package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmadeusServiceTest {
    
    private AmadeusService amadeusService;
    private HttpClient mockHttpClient;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockHttpClient = mock(HttpClient.class);
        
        // Environment variables are already set in build.gradle
        amadeusService = new AmadeusService();
        
        // Use reflection to inject mock HttpClient
        try {
            var httpClientField = AmadeusService.class.getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            httpClientField.set(amadeusService, mockHttpClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock HttpClient", e);
        }
    }
    
    
    @Test
    void getSeatMap_WithValidParameters_ReturnsJsonNode() throws Exception {
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock flight offers response
        HttpResponse<String> flightOffersResponse = mock(HttpResponse.class);
        when(flightOffersResponse.statusCode()).thenReturn(200);
        String flightOffersJson = "{\"data\":[{\"id\":\"offer1\",\"type\":\"flight-offer\"}]}";
        when(flightOffersResponse.body()).thenReturn(flightOffersJson);
        
        // Mock seat map response
        HttpResponse<String> seatMapResponse = mock(HttpResponse.class);
        when(seatMapResponse.statusCode()).thenReturn(200);
        String seatMapJson = "{\"data\":[{\"type\":\"seat-map\",\"flightNumber\":\"AA123\"}]}";
        when(seatMapResponse.body()).thenReturn(seatMapJson);
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse) // First call for token
            .thenReturn(flightOffersResponse) // Second call for flight offers
            .thenReturn(seatMapResponse); // Third call for seat map
        
        JsonNode result = amadeusService.getSeatMap("AA123", "2024-12-01", "LAX", "JFK");
        
        assertNotNull(result);
        assertTrue(result.has("data"));
        verify(mockHttpClient, times(3)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }
    
    @Test
    void getSeatMap_WithInvalidFlightNumber_ThrowsException() throws Exception {
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock error response
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(404);
        when(errorResponse.body()).thenReturn("{\"error\":\"Flight not found\"}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse)
            .thenReturn(errorResponse);
        
        assertThrows(SeatmapApiException.class, () -> 
            amadeusService.getSeatMap("INVALID", "2024-12-01", "LAX", "JFK"));
    }
    
    @Test
    void getSeatMap_WithNetworkError_ThrowsException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenThrow(new IOException("Network error"));
        
        assertThrows(SeatmapApiException.class, () -> 
            amadeusService.getSeatMap("AA123", "2024-12-01", "LAX", "JFK"));
    }
    
    @Test
    void getSeatMap_WithTokenRefreshFailure_ThrowsException() throws Exception {
        // Mock failed token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(401);
        when(tokenResponse.body()).thenReturn("{\"error\":\"Unauthorized\"}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse);
        
        assertThrows(SeatmapApiException.class, () -> 
            amadeusService.getSeatMap("AA123", "2024-12-01", "LAX", "JFK"));
    }
    
    @Test
    void getSeatMap_WithSpecialCharactersInParameters_EncodesCorrectly() throws Exception {
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock flight offers response with data
        HttpResponse<String> flightOffersResponse = mock(HttpResponse.class);
        when(flightOffersResponse.statusCode()).thenReturn(200);
        String flightOffersJson = "{\"data\":[{\"id\":\"offer1\",\"type\":\"flight-offer\"}]}";
        when(flightOffersResponse.body()).thenReturn(flightOffersJson);
        
        // Mock seat map response
        HttpResponse<String> seatMapResponse = mock(HttpResponse.class);
        when(seatMapResponse.statusCode()).thenReturn(200);
        when(seatMapResponse.body()).thenReturn("{\"data\":[{\"type\":\"seat-map\"}]}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse)
            .thenReturn(flightOffersResponse)
            .thenReturn(seatMapResponse);
        
        // Test with special characters that need URL encoding
        assertDoesNotThrow(() -> 
            amadeusService.getSeatMap("AA 123", "2024-12-01", "LAX", "JFK"));
    }
    
    @Test
    void getSeatMap_WithExpiredToken_RefreshesTokenAutomatically() throws Exception {
        // First call - token response
        HttpResponse<String> tokenResponse1 = mock(HttpResponse.class);
        when(tokenResponse1.statusCode()).thenReturn(200);
        when(tokenResponse1.body()).thenReturn("{\"access_token\":\"token1\",\"expires_in\":1}"); // Expires in 1 second
        
        // Second call - new token response
        HttpResponse<String> tokenResponse2 = mock(HttpResponse.class);
        when(tokenResponse2.statusCode()).thenReturn(200);
        when(tokenResponse2.body()).thenReturn("{\"access_token\":\"token2\",\"expires_in\":3600}");
        
        // Flight offers responses
        HttpResponse<String> flightOffersResponse = mock(HttpResponse.class);
        when(flightOffersResponse.statusCode()).thenReturn(200);
        String flightOffersJson = "{\"data\":[{\"id\":\"offer1\",\"type\":\"flight-offer\"}]}";
        when(flightOffersResponse.body()).thenReturn(flightOffersJson);
        
        // Seat map responses
        HttpResponse<String> seatMapResponse = mock(HttpResponse.class);
        when(seatMapResponse.statusCode()).thenReturn(200);
        when(seatMapResponse.body()).thenReturn("{\"data\":[{\"type\":\"seat-map\"}]}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse1)
            .thenReturn(flightOffersResponse)
            .thenReturn(seatMapResponse)
            .thenReturn(tokenResponse2)  // Token refresh
            .thenReturn(flightOffersResponse)
            .thenReturn(seatMapResponse);
        
        // First call
        amadeusService.getSeatMap("AA123", "2024-12-01", "LAX", "JFK");
        
        // Wait for token to expire and make second call
        Thread.sleep(1100);
        amadeusService.getSeatMap("AA124", "2024-12-01", "LAX", "JFK");
        
        // Should have made 6 HTTP calls total (2 tokens + 2 flight offers + 2 seat maps)
        verify(mockHttpClient, times(6)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }
}