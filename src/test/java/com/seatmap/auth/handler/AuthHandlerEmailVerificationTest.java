package com.seatmap.auth.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.service.AuthService;
import com.seatmap.common.exception.SeatmapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthHandlerEmailVerificationTest {
    
    private AuthHandler handler;
    
    @Mock
    private AuthService mockAuthService;
    
    @Mock
    private Context mockContext;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new AuthHandler();
        
        // Use reflection to inject mock
        try {
            var authServiceField = AuthHandler.class.getDeclaredField("authService");
            authServiceField.setAccessible(true);
            authServiceField.set(handler, mockAuthService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock", e);
        }
    }
    
    @Test
    @DisplayName("Should handle email verification successfully")
    void shouldHandleEmailVerificationSuccessfully() throws Exception {
        // Given
        String verificationToken = "valid_verification_token";
        AuthResponse mockResponse = new AuthResponse();
        mockResponse.setToken("jwt_token");
        mockResponse.setExpiresIn(86400);
        mockResponse.setMessage("Email verified successfully!");
        
        when(mockAuthService.verifyEmail(verificationToken)).thenReturn(mockResponse);
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/verify");
        request.setHttpMethod("POST");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("token", verificationToken);
        request.setQueryStringParameters(queryParams);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        
        AuthResponse responseBody = objectMapper.readValue(response.getBody(), AuthResponse.class);
        assertEquals("jwt_token", responseBody.getToken());
        assertTrue(responseBody.getMessage().contains("verified successfully"));
        
        verify(mockAuthService).verifyEmail(verificationToken);
    }
    
    @Test
    @DisplayName("Should return 400 for missing verification token")
    void shouldReturn400ForMissingVerificationToken() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/verify");
        request.setHttpMethod("POST");
        request.setQueryStringParameters(null); // No query parameters
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Verification token required"));
        
        verify(mockAuthService, never()).verifyEmail(anyString());
    }
    
    @Test
    @DisplayName("Should return 400 for invalid verification token")
    void shouldReturn400ForInvalidVerificationToken() throws Exception {
        // Given
        String invalidToken = "invalid_token";
        
        when(mockAuthService.verifyEmail(invalidToken))
            .thenThrow(SeatmapException.badRequest("Invalid or expired verification token"));
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/verify");
        request.setHttpMethod("POST");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("token", invalidToken);
        request.setQueryStringParameters(queryParams);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired verification token"));
        
        verify(mockAuthService).verifyEmail(invalidToken);
    }
    
    @Test
    @DisplayName("Should handle resend verification email successfully")
    void shouldHandleResendVerificationEmailSuccessfully() throws Exception {
        // Given
        String email = "test@example.com";
        AuthResponse mockResponse = new AuthResponse();
        mockResponse.setSuccess(true);
        mockResponse.setMessage("Verification email has been resent.");
        mockResponse.setEmail(email);
        mockResponse.setPending(true);
        
        when(mockAuthService.resendVerificationEmail(email)).thenReturn(mockResponse);
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/resend-verification");
        request.setHttpMethod("POST");
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);
        request.setBody(objectMapper.writeValueAsString(requestBody));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        
        AuthResponse responseBody = objectMapper.readValue(response.getBody(), AuthResponse.class);
        assertTrue(responseBody.isSuccess());
        assertEquals(email, responseBody.getEmail());
        assertTrue(responseBody.isPending());
        assertTrue(responseBody.getMessage().contains("resent"));
        
        verify(mockAuthService).resendVerificationEmail(email);
    }
    
    @Test
    @DisplayName("Should return 400 for missing email in resend request")
    void shouldReturn400ForMissingEmailInResendRequest() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/resend-verification");
        request.setHttpMethod("POST");
        
        Map<String, String> requestBody = new HashMap<>();
        // No email field
        request.setBody(objectMapper.writeValueAsString(requestBody));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Email is required"));
        
        verify(mockAuthService, never()).resendVerificationEmail(anyString());
    }
    
    @Test
    @DisplayName("Should return 400 for empty email in resend request")
    void shouldReturn400ForEmptyEmailInResendRequest() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/resend-verification");
        request.setHttpMethod("POST");
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", "  "); // Empty/whitespace email
        request.setBody(objectMapper.writeValueAsString(requestBody));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Email is required"));
        
        verify(mockAuthService, never()).resendVerificationEmail(anyString());
    }
    
    @Test
    @DisplayName("Should return 404 for non-existent user in resend request")
    void shouldReturn404ForNonExistentUserInResendRequest() throws Exception {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        
        when(mockAuthService.resendVerificationEmail(nonExistentEmail))
            .thenThrow(SeatmapException.notFound("User not found"));
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/resend-verification");
        request.setHttpMethod("POST");
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", nonExistentEmail);
        request.setBody(objectMapper.writeValueAsString(requestBody));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("User not found"));
        
        verify(mockAuthService).resendVerificationEmail(nonExistentEmail);
    }
    
    @Test
    @DisplayName("Should return 400 for already verified user in resend request")
    void shouldReturn400ForAlreadyVerifiedUserInResendRequest() throws Exception {
        // Given
        String verifiedEmail = "verified@example.com";
        
        when(mockAuthService.resendVerificationEmail(verifiedEmail))
            .thenThrow(SeatmapException.badRequest("Email is already verified"));
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/resend-verification");
        request.setHttpMethod("POST");
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", verifiedEmail);
        request.setBody(objectMapper.writeValueAsString(requestBody));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Email is already verified"));
        
        verify(mockAuthService).resendVerificationEmail(verifiedEmail);
    }
    
    @Test
    @DisplayName("Should return 400 for invalid JSON in resend request")
    void shouldReturn400ForInvalidJsonInResendRequest() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/resend-verification");
        request.setHttpMethod("POST");
        request.setBody("invalid json");
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
        
        verify(mockAuthService, never()).resendVerificationEmail(anyString());
    }
    
    @Test
    @DisplayName("Should handle CORS headers correctly for verification endpoints")
    void shouldHandleCorsHeadersCorrectlyForVerificationEndpoints() throws Exception {
        // Given
        String verificationToken = "valid_token";
        AuthResponse mockResponse = new AuthResponse();
        mockResponse.setToken("jwt_token");
        mockResponse.setExpiresIn(86400);
        
        when(mockAuthService.verifyEmail(verificationToken)).thenReturn(mockResponse);
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/verify");
        request.setHttpMethod("POST");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("token", verificationToken);
        request.setQueryStringParameters(queryParams);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        
        Map<String, String> headers = response.getHeaders();
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, POST, PUT, DELETE, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, Authorization, X-API-Key", headers.get("Access-Control-Allow-Headers"));
        assertEquals("application/json", headers.get("Content-Type"));
    }
}