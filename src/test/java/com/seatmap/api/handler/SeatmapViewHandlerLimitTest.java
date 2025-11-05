package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.auth.service.UserUsageLimitsService;
import com.seatmap.common.model.User;
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
class SeatmapViewHandlerLimitTest {

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
    void testGuestExceedsLimit() throws Exception {
        // Given
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(false);
        when(mockGuestAccessRepository.getSeatmapDenialMessage("192.168.1.100"))
            .thenReturn("You've used your 2 free seat map views. Please register for unlimited seat map access.");
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/seatmap/view");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer guest-token");
        headers.put("X-Forwarded-For", "192.168.1.100");
        event.setHeaders(headers);
        event.setBody("{\"flightId\":\"test-flight-123\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(403, response.getStatusCode());
        assertTrue(response.getBody().contains("You've used your 2 free seat map views"));
        
        // Verify limit was checked but usage was NOT recorded
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.100");
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest("192.168.1.100");
    }

    @Test
    void testGuestWithinLimit() throws Exception {
        // Given
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/seatmap/view");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer guest-token");
        headers.put("X-Forwarded-For", "192.168.1.100");
        event.setHeaders(headers);
        event.setBody("{\"flightId\":\"test-flight-123\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Seatmap view recorded"));
        
        // Verify limit was checked AND usage was recorded
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.100");
        verify(mockGuestAccessRepository).recordSeatmapRequest("192.168.1.100");
    }

    @Test
    void testUserExceedsLimit() throws Exception {
        // Given
        User testUser = new User();
        testUser.setUserId("test-user-123");
        testUser.setAccountTier(User.AccountTier.FREE);
        
        when(mockJwtService.validateToken("user-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("user-token")).thenReturn(false);
        when(mockAuthService.validateToken("user-token")).thenReturn(testUser);
        when(mockUserUsageLimitsService.canMakeSeatmapRequest(testUser)).thenReturn(false);
        when(mockUserUsageLimitsService.getSeatmapLimitMessage(testUser))
            .thenReturn("You've reached your monthly limit of 10 seatmap views. Upgrade to Business tier for unlimited access.");
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/seatmap/view");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer user-token");
        event.setHeaders(headers);
        event.setBody("{\"flightId\":\"test-flight-123\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(403, response.getStatusCode());
        assertTrue(response.getBody().contains("You've reached your monthly limit of 10 seatmap views"));
        
        // Verify limit was checked but usage was NOT recorded
        verify(mockUserUsageLimitsService).canMakeSeatmapRequest(testUser);
        verify(mockUserUsageLimitsService, never()).recordSeatmapRequest(testUser);
    }

    @Test
    void testUserWithinLimit() throws Exception {
        // Given
        User testUser = new User();
        testUser.setUserId("test-user-123");
        testUser.setAccountTier(User.AccountTier.FREE);
        
        when(mockJwtService.validateToken("user-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("user-token")).thenReturn(false);
        when(mockAuthService.validateToken("user-token")).thenReturn(testUser);
        when(mockUserUsageLimitsService.canMakeSeatmapRequest(testUser)).thenReturn(true);
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/seatmap/view");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer user-token");
        event.setHeaders(headers);
        event.setBody("{\"flightId\":\"test-flight-123\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Seatmap view recorded"));
        
        // Verify limit was checked AND usage was recorded
        verify(mockUserUsageLimitsService).canMakeSeatmapRequest(testUser);
        verify(mockUserUsageLimitsService).recordSeatmapRequest(testUser);
    }
}