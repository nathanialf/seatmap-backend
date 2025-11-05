package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.auth.service.JwtService;
import com.seatmap.common.exception.SeatmapException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightSearchHandlerTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private JwtService mockJwtService;
    
    @Mock
    private Claims mockClaims;

    private FlightSearchHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        handler = new FlightSearchHandler();
        objectMapper = new ObjectMapper();
        
        // Inject mock JWT service using reflection
        injectMock("jwtService", mockJwtService);
    }
    
    private void injectMock(String fieldName, Object mock) throws Exception {
        Field field = FlightSearchHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, mock);
    }

    @Test
    void testMissingAuthorizationHeader() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        event.setHeaders(new HashMap<>());
        event.setBody("{\"origin\":\"LAX\",\"destination\":\"JFK\",\"departureDate\":\"2024-12-15\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing or invalid authorization header"));
    }

    @Test
    void testInvalidAuthorizationHeader() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Invalid token");
        event.setHeaders(headers);
        event.setBody("{\"origin\":\"LAX\",\"destination\":\"JFK\",\"departureDate\":\"2024-12-15\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing or invalid authorization header"));
    }

    @Test
    void testInvalidRequestBody() throws SeatmapException {
        // Given - Mock JWT to pass validation so we can test JSON parsing
        when(mockJwtService.getUserIdFromToken("test-token")).thenReturn("test-user-id");
        when(mockJwtService.isGuestToken("test-token")).thenReturn(true); // Guest token bypasses validateToken
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        event.setBody("invalid json");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
    }

    @Test
    void testBookmarkRequestWithoutBookmarkId() throws SeatmapException {
        // Given - Mock JWT methods for guest token flow
        when(mockJwtService.getUserIdFromToken("test-token")).thenReturn("test-user-id");
        when(mockJwtService.isGuestToken("test-token")).thenReturn(true);
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        event.setBody(""); // GET request typically has no body

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Should not match bookmark pattern (needs ID) and fall through to regular flow
        // JWT validation passes as guest, then JSON parsing fails
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void testCorsHeaders() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        event.setHeaders(new HashMap<>());
        event.setBody("{}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertNotNull(response.getHeaders());
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
        assertEquals("GET,POST,PUT,DELETE,OPTIONS", response.getHeaders().get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type,Authorization,X-API-Key", response.getHeaders().get("Access-Control-Allow-Headers"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void testValidBookmarkPath() throws SeatmapException {
        // Given - Mock token validation to fail for bookmark access
        doThrow(new SeatmapException("TOKEN_INVALID", "Invalid token", 401))
            .when(mockJwtService).validateToken("test-token");
        
        String bookmarkId = "test-bookmark-123";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/" + bookmarkId);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode()); // Token validation failed
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }
}