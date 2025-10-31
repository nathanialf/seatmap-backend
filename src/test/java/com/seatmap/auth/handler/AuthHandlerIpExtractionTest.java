package com.seatmap.auth.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.service.AuthService;
import com.seatmap.common.exception.SeatmapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthHandlerIpExtractionTest {
    
    private AuthHandler authHandler;
    
    @Mock
    private AuthService mockAuthService;
    
    @Mock
    private Context mockContext;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        authHandler = new AuthHandler();
        
        // Use reflection to inject mock AuthService
        var authServiceField = AuthHandler.class.getDeclaredField("authService");
        authServiceField.setAccessible(true);
        authServiceField.set(authHandler, mockAuthService);
        
        // Mock successful guest session creation
        AuthResponse mockResponse = AuthResponse.forGuest("guest-token", "guest-123", 86400);
        when(mockAuthService.createGuestSession(anyString())).thenReturn(mockResponse);
    }
    
    @Test
    void extractClientIp_FromXForwardedFor_ReturnsFirstIp() throws Exception {
        // Setup request with X-Forwarded-For header
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "203.0.113.195, 192.168.1.1, 10.0.0.1");
        request.setHeaders(headers);
        
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        
        // Verify AuthService was called with the first IP from X-Forwarded-For
        verify(mockAuthService).createGuestSession("203.0.113.195");
    }
    
    @Test
    void extractClientIp_FromXForwardedFor_SingleIp() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "203.0.113.100");
        request.setHeaders(headers);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("203.0.113.100");
    }
    
    @Test
    void extractClientIp_FromXForwardedFor_WithSpaces() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "  203.0.113.200  , 192.168.1.1  ");
        request.setHeaders(headers);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("203.0.113.200");
    }
    
    @Test
    void extractClientIp_FromXRealIp_WhenXForwardedForMissing() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Real-IP", "203.0.113.150");
        request.setHeaders(headers);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("203.0.113.150");
    }
    
    @Test
    void extractClientIp_FromSourceIp_WhenHeadersMissing() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        // Mock request context with source IP
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        APIGatewayProxyRequestEvent.RequestIdentity identity = new APIGatewayProxyRequestEvent.RequestIdentity();
        identity.setSourceIp("203.0.113.75");
        context.setIdentity(identity);
        request.setRequestContext(context);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("203.0.113.75");
    }
    
    @Test
    void extractClientIp_FallbackToUnknown_WhenAllSourcesMissing() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        request.setRequestContext(null);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("unknown");
    }
    
    @Test
    void extractClientIp_EmptyXForwardedFor_FallsBackToSourceIp() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "   "); // Empty/whitespace only
        request.setHeaders(headers);
        
        // Set up source IP fallback
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        APIGatewayProxyRequestEvent.RequestIdentity identity = new APIGatewayProxyRequestEvent.RequestIdentity();
        identity.setSourceIp("10.0.0.50");
        context.setIdentity(identity);
        request.setRequestContext(context);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("10.0.0.50");
    }
    
    @Test
    void extractClientIp_NullHeaders_FallsBackToSourceIp() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        request.setHeaders(null);
        
        // Set up source IP fallback
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        APIGatewayProxyRequestEvent.RequestIdentity identity = new APIGatewayProxyRequestEvent.RequestIdentity();
        identity.setSourceIp("172.16.0.10");
        context.setIdentity(identity);
        request.setRequestContext(context);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("172.16.0.10");
    }
    
    @Test
    void extractClientIp_PreferXForwardedForOverXRealIp() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "203.0.113.1");
        headers.put("X-Real-IP", "203.0.113.2");
        request.setHeaders(headers);
        
        authHandler.handleRequest(request, mockContext);
        
        // Should prefer X-Forwarded-For over X-Real-IP
        verify(mockAuthService).createGuestSession("203.0.113.1");
    }
    
    @Test
    void extractClientIp_IPv6Address_HandledCorrectly() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "2001:db8::1");
        request.setHeaders(headers);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("2001:db8::1");
    }
    
    @Test
    void extractClientIp_LocalhostAddress_HandledCorrectly() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "127.0.0.1");
        request.setHeaders(headers);
        
        authHandler.handleRequest(request, mockContext);
        
        verify(mockAuthService).createGuestSession("127.0.0.1");
    }
    
    @Test
    void ipExtraction_PropagatesAuthServiceExceptions() throws Exception {
        String testIp = "192.168.1.100";
        
        // Mock AuthService to throw forbidden exception
        when(mockAuthService.createGuestSession(testIp))
            .thenThrow(SeatmapException.forbidden("IP limit exceeded"));
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", testIp);
        request.setHeaders(headers);
        
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, mockContext);
        
        assertEquals(403, response.getStatusCode());
        assertTrue(response.getBody().contains("IP limit exceeded"));
        
        verify(mockAuthService).createGuestSession(testIp);
    }
    
    @Test
    void ipExtraction_IntegrationWithGuestSessionFlow() throws Exception {
        String testIp = "203.0.113.99";
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", testIp);
        request.setHeaders(headers);
        
        APIGatewayProxyResponseEvent response = authHandler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        
        // Verify response contains guest token
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("guest-token", responseBody.get("token").asText());
        
        // Verify correct IP was passed to AuthService
        verify(mockAuthService).createGuestSession(testIp);
    }
    
    @Test
    void extractClientIp_DirectAccess_UsingReflection() throws Exception {
        // This test calls extractClientIp directly via reflection, not createGuestSession
        // So we need to reset the mock to avoid unnecessary stubbing warnings
        reset(mockAuthService);
        
        // Test the private extractClientIp method directly using reflection
        Method extractMethod = AuthHandler.class.getDeclaredMethod("extractClientIp", APIGatewayProxyRequestEvent.class);
        extractMethod.setAccessible(true);
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-For", "198.51.100.1, 203.0.113.1");
        request.setHeaders(headers);
        
        String extractedIp = (String) extractMethod.invoke(authHandler, request);
        
        assertEquals("198.51.100.1", extractedIp);
    }
}