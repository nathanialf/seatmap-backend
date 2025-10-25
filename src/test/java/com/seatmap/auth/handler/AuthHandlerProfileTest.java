package com.seatmap.auth.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.auth.model.ProfileRequest;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthHandlerProfileTest {
    
    private AuthHandler handler;
    
    @Mock
    private AuthService mockAuthService;
    
    @Mock
    private UserRepository mockUserRepository;
    
    @Mock
    private Context mockContext;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new AuthHandler();
        
        // Use reflection to inject mocks
        try {
            var authServiceField = AuthHandler.class.getDeclaredField("authService");
            authServiceField.setAccessible(true);
            authServiceField.set(handler, mockAuthService);
            
            var userRepositoryField = AuthHandler.class.getDeclaredField("userRepository");
            userRepositoryField.setAccessible(true);
            userRepositoryField.set(handler, mockUserRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }
    
    @Test
    void handleGetProfile_WithValidToken_ReturnsUserProfile() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/profile");
        request.setHttpMethod("GET");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer valid-user-token");
        request.setHeaders(headers);
        
        // Create mock user
        User mockUser = new User();
        mockUser.setUserId("user123");
        mockUser.setEmail("user@example.com");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");
        mockUser.setEmailVerified(true);
        mockUser.setCreatedAt(Instant.now());
        mockUser.setUpdatedAt(Instant.now());
        
        // Mock auth service to return user
        when(mockAuthService.validateToken("valid-user-token")).thenReturn(mockUser);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("user@example.com"));
        assertTrue(response.getBody().contains("John"));
        assertTrue(response.getBody().contains("Doe"));
        assertFalse(response.getBody().contains("passwordHash")); // Should not include sensitive fields
        verify(mockAuthService).validateToken("valid-user-token");
    }
    
    @Test
    void handleGetProfile_WithGuestToken_ReturnsUnauthorized() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/profile");
        request.setHttpMethod("GET");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer guest-token");
        request.setHeaders(headers);
        
        // Mock auth service to return null for guest token
        when(mockAuthService.validateToken("guest-token")).thenReturn(null);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or guest token"));
        verify(mockAuthService).validateToken("guest-token");
    }
    
    @Test
    void handleGetProfile_WithMissingToken_ReturnsUnauthorized() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/profile");
        request.setHttpMethod("GET");
        request.setHeaders(new HashMap<>());
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
        verifyNoInteractions(mockAuthService);
    }
    
    @Test
    void handleUpdateProfile_WithValidData_UpdatesAndReturnsProfile() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/profile");
        request.setHttpMethod("PUT");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer valid-user-token");
        request.setHeaders(headers);
        
        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setFirstName("Jane");
        profileRequest.setLastName("Smith");
        profileRequest.setProfilePicture("https://example.com/photo.jpg");
        request.setBody(objectMapper.writeValueAsString(profileRequest));
        
        // Create mock user
        User mockUser = new User();
        mockUser.setUserId("user123");
        mockUser.setEmail("user@example.com");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");
        mockUser.setEmailVerified(true);
        
        // Mock auth service to return user
        when(mockAuthService.validateToken("valid-user-token")).thenReturn(mockUser);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Jane"));
        assertTrue(response.getBody().contains("Smith"));
        assertTrue(response.getBody().contains("https://example.com/photo.jpg"));
        
        verify(mockAuthService).validateToken("valid-user-token");
        verify(mockUserRepository).saveUser(any(User.class));
        
        // Verify user was updated
        assertEquals("Jane", mockUser.getFirstName());
        assertEquals("Smith", mockUser.getLastName());
        assertEquals("https://example.com/photo.jpg", mockUser.getProfilePicture());
    }
    
    @Test
    void handleUpdateProfile_WithPartialData_UpdatesOnlyProvidedFields() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/profile");
        request.setHttpMethod("PUT");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer valid-user-token");
        request.setHeaders(headers);
        
        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setFirstName("Jane");
        // Only updating first name, leaving last name and profile picture null
        request.setBody(objectMapper.writeValueAsString(profileRequest));
        
        // Create mock user
        User mockUser = new User();
        mockUser.setUserId("user123");
        mockUser.setEmail("user@example.com");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");
        mockUser.setProfilePicture("https://old-photo.jpg");
        
        // Mock auth service to return user
        when(mockAuthService.validateToken("valid-user-token")).thenReturn(mockUser);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        
        verify(mockAuthService).validateToken("valid-user-token");
        verify(mockUserRepository).saveUser(any(User.class));
        
        // Verify only first name was updated
        assertEquals("Jane", mockUser.getFirstName());
        assertEquals("Doe", mockUser.getLastName()); // Should remain unchanged
        assertEquals("https://old-photo.jpg", mockUser.getProfilePicture()); // Should remain unchanged
    }
    
    @Test
    void handleUpdateProfile_WithInvalidData_ReturnsBadRequest() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/profile");
        request.setHttpMethod("PUT");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer valid-user-token");
        request.setHeaders(headers);
        
        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setFirstName("A".repeat(51)); // Exceeds 50 character limit
        request.setBody(objectMapper.writeValueAsString(profileRequest));
        
        // Create mock user
        User mockUser = new User();
        mockUser.setUserId("user123");
        mockUser.setEmail("user@example.com");
        
        // Mock auth service to return user
        when(mockAuthService.validateToken("valid-user-token")).thenReturn(mockUser);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Validation errors"));
        
        verify(mockAuthService).validateToken("valid-user-token");
        verifyNoInteractions(mockUserRepository); // Should not save invalid data
    }
    
    @Test
    void handleUpdateProfile_WithGuestToken_ReturnsUnauthorized() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/profile");
        request.setHttpMethod("PUT");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer guest-token");
        request.setHeaders(headers);
        
        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setFirstName("Jane");
        request.setBody(objectMapper.writeValueAsString(profileRequest));
        
        // Mock auth service to return null for guest token
        when(mockAuthService.validateToken("guest-token")).thenReturn(null);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or guest token"));
        verify(mockAuthService).validateToken("guest-token");
        verifyNoInteractions(mockUserRepository);
    }
    
    @Test
    void handleUpdateProfile_WithInvalidJson_ReturnsBadRequest() throws Exception {
        // Setup request
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setPath("/auth/profile");
        request.setHttpMethod("PUT");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer valid-user-token");
        request.setHeaders(headers);
        
        request.setBody("{invalid json}");
        
        // Create mock user
        User mockUser = new User();
        mockUser.setUserId("user123");
        
        // Mock auth service to return user
        when(mockAuthService.validateToken("valid-user-token")).thenReturn(mockUser);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
        
        verify(mockAuthService).validateToken("valid-user-token");
        verifyNoInteractions(mockUserRepository);
    }
}