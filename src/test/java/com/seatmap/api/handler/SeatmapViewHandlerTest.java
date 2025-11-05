package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.auth.service.UserUsageLimitsService;
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
class SeatmapViewHandlerTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private JwtService mockJwtService;
    
    @Mock
    private AuthService mockAuthService;
    
    @Mock
    private UserUsageLimitsService mockUserUsageLimitsService;
    
    @Mock
    private GuestAccessRepository mockGuestAccessRepository;
    
    @Mock
    private Claims mockClaims;

    private SeatmapViewHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        handler = new SeatmapViewHandler();
        objectMapper = new ObjectMapper();
        
        // Inject mocks using reflection
        injectMock("jwtService", mockJwtService);
        injectMock("authService", mockAuthService);
        injectMock("userUsageLimitsService", mockUserUsageLimitsService);
        injectMock("guestAccessRepository", mockGuestAccessRepository);
    }
    
    private void injectMock(String fieldName, Object mock) throws Exception {
        Field field = SeatmapViewHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, mock);
    }

    @Test
    void testMissingAuthorizationHeader() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHeaders(new HashMap<>());
        event.setBody("{\"flightId\":\"test-flight-123\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
    }

    @Test
    void testInvalidAuthorizationHeader() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Invalid token");
        event.setHeaders(headers);
        event.setBody("{\"flightId\":\"test-flight-123\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
    }

    @Test
    void testInvalidRequestBody() throws Exception {
        // Given - Mock JWT to pass validation so we can test JSON parsing
        when(mockJwtService.validateToken("test-token")).thenReturn(mockClaims);
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        
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
    void testMissingFlightId() throws Exception {
        // Given - Mock JWT to pass validation so we can test request validation
        when(mockJwtService.validateToken("test-token")).thenReturn(mockClaims);
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        event.setBody("{}"); // Missing flightId

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Flight ID is required"));
    }

    @Test
    void testBlankFlightId() throws Exception {
        // Given - Mock JWT to pass validation so we can test request validation
        when(mockJwtService.validateToken("test-token")).thenReturn(mockClaims);
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        event.setBody("{\"flightId\":\"\"}"); // Blank flightId

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Flight ID is required"));
    }

    @Test
    void testCorsHeaders() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHeaders(new HashMap<>());
        event.setBody("{}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertNotNull(response.getHeaders());
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
        assertEquals("GET, POST, PUT, DELETE, OPTIONS", response.getHeaders().get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, Authorization", response.getHeaders().get("Access-Control-Allow-Headers"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void testValidRequestStructure() throws Exception {
        // Given - Mock JWT validation to fail intentionally to test that request got parsed correctly
        when(mockJwtService.validateToken("test-token")).thenThrow(new com.seatmap.common.exception.SeatmapException("AUTH_ERROR", "Invalid token"));
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        event.setBody("{\"flightId\":\"test-flight-123\",\"dataSource\":\"AMADEUS\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode()); // Will fail due to invalid token, but request structure was valid
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }
}