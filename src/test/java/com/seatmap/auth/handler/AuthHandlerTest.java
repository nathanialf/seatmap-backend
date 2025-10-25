package com.seatmap.auth.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.model.LoginRequest;
import com.seatmap.auth.model.RegisterRequest;
import com.seatmap.auth.service.AuthService;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthHandlerTest {
    
    private AuthHandler handler;
    
    @Mock
    private AuthService mockAuthService;
    
    @Mock
    private EmailService mockEmailService;
    
    @Mock
    private Context mockContext;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new AuthHandler();
        
        // Use reflection to inject mocks
        try {
            var authServiceField = AuthHandler.class.getDeclaredField("authService");
            authServiceField.setAccessible(true);
            authServiceField.set(handler, mockAuthService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock", e);
        }
    }
    
    @Test
    void handleRequest_GuestEndpoint_ReturnsGuestToken() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        // Mock auth service response
        AuthResponse mockResponse = AuthResponse.forGuest("guest-token", "guest-123", 86400);
        mockResponse.setMessage("Guest session created. You can view up to 2 seat maps.");
        when(mockAuthService.createGuestSession(anyString())).thenReturn(mockResponse);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("guest-token"));
        assertTrue(response.getBody().contains("Guest session created"));
        verify(mockAuthService).createGuestSession(anyString());
    }
    
    @Test
    void handleRequest_LoginEndpoint_WithValidCredentials_ReturnsUserToken() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/login");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password123");
        request.setBody(objectMapper.writeValueAsString(loginRequest));
        
        // Mock auth service response
        AuthResponse mockResponse = new AuthResponse();
        mockResponse.setToken("user-token");
        mockResponse.setUserId("user-123");
        mockResponse.setEmail("user@example.com");
        mockResponse.setExpiresIn(86400);
        when(mockAuthService.login(any(LoginRequest.class))).thenReturn(mockResponse);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("user-token"));
        assertTrue(response.getBody().contains("user@example.com"));
        verify(mockAuthService).login(any(LoginRequest.class));
    }
    
    @Test
    void handleRequest_LoginEndpoint_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/login");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        LoginRequest loginRequest = new LoginRequest("user@example.com", "wrongpassword");
        request.setBody(objectMapper.writeValueAsString(loginRequest));
        
        // Mock auth service to throw exception
        when(mockAuthService.login(any(LoginRequest.class)))
            .thenThrow(SeatmapException.unauthorized("Invalid email or password"));
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid email or password"));
        verify(mockAuthService).login(any(LoginRequest.class));
    }
    
    @Test
    void handleRequest_RegisterEndpoint_WithValidData_ReturnsUserToken() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/register");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        request.setBody(objectMapper.writeValueAsString(registerRequest));
        
        // Mock auth service response - now returns pending response without JWT
        AuthResponse mockResponse = new AuthResponse();
        mockResponse.setSuccess(true);
        mockResponse.setPending(true);
        mockResponse.setEmail("newuser@example.com");
        mockResponse.setMessage("Registration successful! Please check your email to verify your account.");
        when(mockAuthService.register(any(RegisterRequest.class))).thenReturn(mockResponse);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Registration successful"));
        assertTrue(response.getBody().contains("newuser@example.com"));
        assertTrue(response.getBody().contains("\"token\":null"));
        assertTrue(response.getBody().contains("\"pending\":true"));
        verify(mockAuthService).register(any(RegisterRequest.class));
    }
    
    @Test
    void handleRequest_RegisterEndpoint_WithExistingEmail_ReturnsConflict() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/register");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("existing@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        request.setBody(objectMapper.writeValueAsString(registerRequest));
        
        // Mock auth service to throw bad request exception (changed from conflict)
        when(mockAuthService.register(any(RegisterRequest.class)))
            .thenThrow(SeatmapException.badRequest("Email address is already registered"));
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Email address is already registered"));
        verify(mockAuthService).register(any(RegisterRequest.class));
    }
    
    @Test
    void handleRequest_RefreshEndpoint_WithValidToken_ReturnsNewToken() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/refresh");
        request.setHttpMethod("POST");
        request.setHeaders(Map.of("Authorization", "Bearer old-token"));
        
        // Mock auth service response
        AuthResponse mockResponse = new AuthResponse();
        mockResponse.setToken("refreshed-token");
        mockResponse.setExpiresIn(86400);
        when(mockAuthService.refreshToken("old-token")).thenReturn(mockResponse);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("refreshed-token"));
        verify(mockAuthService).refreshToken("old-token");
    }
    
    @Test
    void handleRequest_RefreshEndpoint_WithMissingToken_ReturnsUnauthorized() {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/refresh");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
        verifyNoInteractions(mockAuthService);
    }
    
    @Test
    void handleRequest_LogoutEndpoint_WithValidToken_ReturnsSuccess() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/logout");
        request.setHttpMethod("DELETE");
        request.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        // Mock auth service (logout returns void)
        doNothing().when(mockAuthService).logout("valid-token");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Logged out successfully"));
        verify(mockAuthService).logout("valid-token");
    }
    
    @Test
    void handleRequest_InvalidEndpoint_ReturnsNotFound() {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/invalid");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Endpoint not found"));
        verifyNoInteractions(mockAuthService);
    }
    
    @Test
    void handleRequest_UnsupportedMethod_ReturnsMethodNotAllowed() {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("GET");
        request.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(405, response.getStatusCode());
        assertTrue(response.getBody().contains("Method not allowed"));
        verifyNoInteractions(mockAuthService);
    }
    
    @Test
    void handleRequest_WithInvalidJson_ReturnsBadRequest() {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/login");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        request.setBody("{invalid json}");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
        verifyNoInteractions(mockAuthService);
    }
    
    @Test
    void handleRequest_WithValidationErrors_ReturnsBadRequest() throws Exception {
        // Setup request with invalid email
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/login");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        LoginRequest invalidRequest = new LoginRequest("invalid-email", "");
        request.setBody(objectMapper.writeValueAsString(invalidRequest));
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Validation errors"));
        verifyNoInteractions(mockAuthService);
    }
    
    @Test
    void handleRequest_WithUnexpectedException_ReturnsInternalServerError() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        // Mock auth service to throw unexpected exception
        when(mockAuthService.createGuestSession("unknown"))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Internal server error"));
        verify(mockAuthService).createGuestSession("unknown");
    }
    
    @Test
    void handleRequest_AllResponsesHaveCorsHeaders() throws Exception {
        // Test that all responses include CORS headers
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/guest");
        request.setHttpMethod("POST");
        request.setHeaders(new HashMap<>());
        
        AuthResponse mockResponse = AuthResponse.forGuest("token", "guest-123", 86400);
        when(mockAuthService.createGuestSession()).thenReturn(mockResponse);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertNotNull(response.getHeaders());
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
        assertEquals("GET, POST, PUT, DELETE, OPTIONS", response.getHeaders().get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, Authorization, X-API-Key", response.getHeaders().get("Access-Control-Allow-Headers"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }
}