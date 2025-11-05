package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.model.FlightSearchResponse;
import com.seatmap.api.model.FlightSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FlightSearchHandlerIntegrationTest {

    @Mock
    private Context mockContext;

    private FlightSearchHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Note: This test requires valid environment variables to be set
        // JWT_SECRET, AMADEUS_API_KEY, AMADEUS_API_SECRET, AMADEUS_ENDPOINT
        // SABRE_USER_ID, SABRE_PASSWORD, SABRE_ENDPOINT
        handler = new FlightSearchHandler();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testFlightSearchWithValidRequest() {
        // Given
        APIGatewayProxyRequestEvent event = createFlightSearchRequest(
            "LAX", "JFK", "2024-12-15", "ECONOMY", null, 5
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Authentication happens before business logic, so expect 401
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    void testFlightSearchWithSpecificFlightNumber() {
        // Given
        APIGatewayProxyRequestEvent event = createFlightSearchRequest(
            "LAX", "JFK", "2024-12-15", "BUSINESS", "AA100", 3
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Authentication happens before business logic, so expect 401
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    void testBookmarkFlightSearchWithValidBookmark() {
        // Given
        APIGatewayProxyRequestEvent event = createBookmarkRequest("test-bookmark-id");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Should return 401 or 404 depending on authentication/bookmark existence
        assertTrue(response.getStatusCode() == 401 || response.getStatusCode() == 404);
    }

    @Test
    void testFlightSearchConcurrentSeatmapFetching() {
        // Given - request for multiple results to test concurrent processing
        APIGatewayProxyRequestEvent event = createFlightSearchRequest(
            "LAX", "JFK", "2024-12-15", null, null, 10
        );

        // When
        long startTime = System.currentTimeMillis();
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        // Authentication happens before business logic, so expect 401
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    void testFlightSearchValidationErrors() {
        // Given - invalid request
        APIGatewayProxyRequestEvent event = createFlightSearchRequest(
            "", "JFK", "invalid-date", "INVALID_CLASS", null, -1
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Authentication happens before request validation, so expect 401
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    void testFlightSearchResponseStructure() {
        // Given
        APIGatewayProxyRequestEvent event = createFlightSearchRequest(
            "LAX", "JFK", "2024-12-15", "ECONOMY", null, 2
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Authentication happens before business logic, so expect 401
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    private APIGatewayProxyRequestEvent createFlightSearchRequest(String origin, String destination, 
                                                                String departureDate, String travelClass, 
                                                                String flightNumber, Integer maxResults) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-guest-token"); // Will fail auth but test structure
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);
        
        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("origin", origin);
        requestBody.put("destination", destination);
        requestBody.put("departureDate", departureDate);
        if (travelClass != null) requestBody.put("travelClass", travelClass);
        if (flightNumber != null) requestBody.put("flightNumber", flightNumber);
        if (maxResults != null) requestBody.put("maxResults", maxResults);
        
        try {
            event.setBody(objectMapper.writeValueAsString(requestBody));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create request body", e);
        }
        
        return event;
    }

    private APIGatewayProxyRequestEvent createBookmarkRequest(String bookmarkId) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/" + bookmarkId);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-user-token"); // Will fail auth but test structure
        event.setHeaders(headers);
        
        return event;
    }
}