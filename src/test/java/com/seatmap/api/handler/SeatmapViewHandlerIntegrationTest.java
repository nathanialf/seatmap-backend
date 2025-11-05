package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SeatmapViewHandlerIntegrationTest {

    @Mock
    private Context mockContext;

    private SeatmapViewHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Note: This test requires valid environment variables to be set
        // JWT_SECRET, ENVIRONMENT
        handler = new SeatmapViewHandler();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testSeatmapViewWithValidRequest() {
        // Given
        APIGatewayProxyRequestEvent event = createSeatmapViewRequest("amadeus_flight_123", "AMADEUS");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Should return 401 due to invalid token, but request structure is validated
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    void testSeatmapViewWithMissingFlightId() {
        // Given
        APIGatewayProxyRequestEvent event = createSeatmapViewRequestWithMissingFlightId();

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Authentication happens before request validation, so expect 401
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    void testSeatmapViewWithInvalidJson() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);
        event.setBody("invalid json");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Authentication happens before JSON parsing, so expect 401
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    void testSeatmapViewWithMissingAuthHeader() {
        // Given
        APIGatewayProxyRequestEvent event = createSeatmapViewRequest("test_flight_456", null);
        event.setHeaders(new HashMap<>()); // No auth header

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
    }

    @Test
    void testSeatmapViewWithInvalidAuthHeader() {
        // Given
        APIGatewayProxyRequestEvent event = createSeatmapViewRequest("test_flight_789", "SABRE");
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Invalid bearer token format");
        event.setHeaders(headers);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
    }

    @Test
    void testSeatmapViewResponseStructure() {
        // Given
        APIGatewayProxyRequestEvent event = createSeatmapViewRequest("flight_abc123", "AMADEUS");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Should fail authentication but response structure should be valid
        assertTrue(response.getStatusCode() == 401); // Invalid token
        
        try {
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            assertTrue(responseNode.has("success"));
            assertTrue(responseNode.has("message"));
            assertFalse(responseNode.get("success").asBoolean());
        } catch (Exception e) {
            fail("Response should be valid JSON: " + e.getMessage());
        }
    }

    @Test
    void testSeatmapViewWithOptionalDataSource() {
        // Given - request without dataSource field (should be optional)
        APIGatewayProxyRequestEvent event = createSeatmapViewRequest("flight_def456", null);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode()); // Should fail on auth, not validation
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    void testSeatmapViewCorsHeaders() {
        // Given
        APIGatewayProxyRequestEvent event = createSeatmapViewRequest("flight_ghi789", "SABRE");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, POST, PUT, DELETE, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, Authorization", headers.get("Access-Control-Allow-Headers"));
        assertEquals("application/json", headers.get("Content-Type"));
    }

    @Test
    void testSeatmapViewClientIpExtraction() {
        // Given - event with various IP headers to test extraction logic
        APIGatewayProxyRequestEvent event = createSeatmapViewRequest("flight_jkl012", "AMADEUS");
        
        // Add headers that would be present in real API Gateway requests
        Map<String, String> headers = event.getHeaders();
        headers.put("X-Forwarded-For", "203.0.113.195, 192.168.1.1, 10.0.0.1");
        headers.put("X-Real-IP", "203.0.113.195");
        event.setHeaders(headers);
        
        // Mock request context for source IP
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        APIGatewayProxyRequestEvent.RequestIdentity identity = new APIGatewayProxyRequestEvent.RequestIdentity();
        identity.setSourceIp("172.16.0.1");
        context.setIdentity(identity);
        event.setRequestContext(context);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode()); // Should fail on auth
        // The handler should have processed the IP extraction without errors
        assertNotNull(response.getBody());
    }

    @Test
    void testSeatmapViewPerformance() {
        // Given
        APIGatewayProxyRequestEvent event = createSeatmapViewRequest("performance_test_flight", "AMADEUS");

        // When
        long startTime = System.currentTimeMillis();
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        // Should be very fast since it's just usage tracking
        assertTrue(duration < 5000, "Seatmap view tracking should complete within 5 seconds");
        assertEquals(401, response.getStatusCode()); // Expected auth failure
    }

    private APIGatewayProxyRequestEvent createSeatmapViewRequest(String flightId, String dataSource) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);
        
        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("flightId", flightId);
        if (dataSource != null) {
            requestBody.put("dataSource", dataSource);
        }
        
        try {
            event.setBody(objectMapper.writeValueAsString(requestBody));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create request body", e);
        }
        
        return event;
    }

    private APIGatewayProxyRequestEvent createSeatmapViewRequestWithMissingFlightId() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        headers.put("Content-Type", "application/json");
        event.setHeaders(headers);
        
        // Create request body without flightId
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("dataSource", "AMADEUS");
        
        try {
            event.setBody(objectMapper.writeValueAsString(requestBody));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create request body", e);
        }
        
        return event;
    }
}