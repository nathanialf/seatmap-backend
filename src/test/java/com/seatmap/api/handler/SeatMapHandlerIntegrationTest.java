package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapException;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.api.service.SabreService;
import com.seatmap.auth.repository.BookmarkRepository;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.service.JwtService;
import com.seatmap.common.model.Bookmark;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for SeatMapHandler to boost coverage from 34% to 80%
 * Focus areas:
 * 1. IP extraction logic with various header combinations
 * 2. Sabre vs Amadeus routing based on flight data source
 * 3. Guest access tracking and recording
 * 4. Error handling edge cases
 * 5. Response formatting scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeatMapHandler Integration Tests")
class SeatMapHandlerIntegrationTest {

    private SeatMapHandler handler;
    
    @Mock
    private AmadeusService mockAmadeusService;
    
    @Mock
    private SabreService mockSabreService;
    
    @Mock
    private JwtService mockJwtService;
    
    @Mock
    private GuestAccessRepository mockGuestAccessRepository;
    
    @Mock
    private BookmarkRepository mockBookmarkRepository;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private Claims mockClaims;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new SeatMapHandler();
        
        // Use reflection to inject mocks
        try {
            injectMock("amadeusService", mockAmadeusService);
            injectMock("sabreService", mockSabreService);
            injectMock("jwtService", mockJwtService);
            injectMock("guestAccessRepository", mockGuestAccessRepository);
            injectMock("bookmarkRepository", mockBookmarkRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }
    
    private void injectMock(String fieldName, Object mock) throws Exception {
        var field = SeatMapHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, mock);
    }

    // ========== IP EXTRACTION COMPREHENSIVE TESTS ==========
    
    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header with single IP")
    void extractClientIp_FromXForwardedFor_SingleIp() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "192.168.1.100"
        ));
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // When
        handler.handleRequest(request, mockContext);
        
        // Then
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.100");
    }
    
    @Test
    @DisplayName("Should extract first IP from X-Forwarded-For header with multiple IPs")
    void extractClientIp_FromXForwardedFor_MultipleIps() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "192.168.1.100, 10.0.0.1, 172.16.0.1"
        ));
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // When
        handler.handleRequest(request, mockContext);
        
        // Then
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.100");
    }
    
    @Test
    @DisplayName("Should extract IP from X-Forwarded-For with spaces")
    void extractClientIp_FromXForwardedFor_WithSpaces() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "  192.168.1.100  ,  10.0.0.1  "
        ));
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // When
        handler.handleRequest(request, mockContext);
        
        // Then
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.100");
    }
    
    @Test
    @DisplayName("Should fallback to X-Real-IP when X-Forwarded-For is missing")
    void extractClientIp_FallbackToXRealIp() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Real-IP", "192.168.1.200"
        ));
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.200")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // When
        handler.handleRequest(request, mockContext);
        
        // Then
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.200");
    }
    
    @Test
    @DisplayName("Should prefer X-Forwarded-For over X-Real-IP")
    void extractClientIp_PreferXForwardedForOverXRealIp() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "192.168.1.100",
            "X-Real-IP", "192.168.1.200"
        ));
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // When
        handler.handleRequest(request, mockContext);
        
        // Then
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("192.168.1.100");
        verify(mockGuestAccessRepository, never()).canMakeSeatmapRequest("192.168.1.200");
    }
    
    @Test
    @DisplayName("Should fallback to source IP when headers are missing")
    void extractClientIp_FallbackToSourceIp() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequestWithContext();
        request.setHeaders(Map.of("Authorization", "Bearer guest-token"));
        request.getRequestContext().getIdentity().setSourceIp("10.0.0.50");
        
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("10.0.0.50")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // When
        handler.handleRequest(request, mockContext);
        
        // Then
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("10.0.0.50");
    }
    
    @Test
    @DisplayName("Should fallback to unknown when all sources are missing")
    void extractClientIp_FallbackToUnknown() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer guest-token"));
        
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("unknown")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // When
        handler.handleRequest(request, mockContext);
        
        // Then
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("unknown");
    }
    
    @Test
    @DisplayName("Should handle empty X-Forwarded-For and fallback to source IP")
    void extractClientIp_EmptyXForwardedFor_FallsBackToSourceIp() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequestWithContext();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", ""
        ));
        request.getRequestContext().getIdentity().setSourceIp("10.0.0.75");
        
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("10.0.0.75")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // When
        handler.handleRequest(request, mockContext);
        
        // Then
        verify(mockGuestAccessRepository).canMakeSeatmapRequest("10.0.0.75");
    }

    // ========== SABRE VS AMADEUS ROUTING TESTS ==========
    
    @Test
    @DisplayName("Should route to Sabre when dataSource is SABRE")
    void getSeatMapBySource_RouteToSabre() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        String sabreFlightData = createSabreFlightOfferData();
        request.setBody(createSeatMapRequestBody(sabreFlightData));
        
        setupValidUserToken();
        JsonNode mockSabreResponse = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\",\"source\":\"sabre\"}]}");
        when(mockSabreService.getSeatMapFromFlight("AA", "123", "2024-12-01", "LAX", "JFK"))
            .thenReturn(mockSabreResponse);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockSabreService).getSeatMapFromFlight("AA", "123", "2024-12-01", "LAX", "JFK");
        verify(mockAmadeusService, never()).getSeatMapFromOfferData(anyString());
    }
    
    @Test
    @DisplayName("Should route to Amadeus when dataSource is AMADEUS")
    void getSeatMapBySource_RouteToAmadeus() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        String amadeusFlightData = createAmadeusFlightOfferData();
        request.setBody(createSeatMapRequestBody(amadeusFlightData));
        
        setupValidUserToken();
        setupValidAmadeusResponse();
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockAmadeusService).getSeatMapFromOfferData(anyString());
        verify(mockSabreService, never()).getSeatMapFromFlight(anyString(), anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should fallback to Amadeus when dataSource is missing")
    void getSeatMapBySource_FallbackToAmadeusWhenSourceMissing() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        String flightDataWithoutSource = "{\"id\":\"offer123\",\"type\":\"flight-offer\"}";
        request.setBody(createSeatMapRequestBody(flightDataWithoutSource));
        
        setupValidUserToken();
        setupValidAmadeusResponse();
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockAmadeusService).getSeatMapFromOfferData(anyString());
        verify(mockSabreService, never()).getSeatMapFromFlight(anyString(), anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should fallback to Amadeus when flight data parsing fails")
    void getSeatMapBySource_FallbackToAmadeusOnParsingError() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        String invalidFlightData = "invalid-json-data";
        request.setBody(createSeatMapRequestBody(invalidFlightData));
        
        setupValidUserToken();
        setupValidAmadeusResponse();
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockAmadeusService).getSeatMapFromOfferData(anyString());
    }
    
    @Test
    @DisplayName("Should handle Sabre service errors gracefully")
    void getSeatMapFromSabre_ServiceError_HandledGracefully() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        String sabreFlightData = createSabreFlightOfferData();
        request.setBody(createSeatMapRequestBody(sabreFlightData));
        
        setupValidUserToken();
        
        // Mock Sabre service to throw error
        when(mockSabreService.getSeatMapFromFlight(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new SeatmapException("Sabre API unavailable"));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Sabre API unavailable"));
    }

    // ========== GUEST ACCESS RECORDING TESTS ==========
    
    @Test
    @DisplayName("Should record seatmap request for guest when valid seat map data is returned")
    void guestAccessRecording_ValidSeatMapData_RecordsRequest() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "192.168.1.100"
        ));
        
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        
        // Mock Amadeus returning valid seat map data
        JsonNode validSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"},{\"seat\":\"1B\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString())).thenReturn(validSeatMapData);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockGuestAccessRepository).recordSeatmapRequest("192.168.1.100");
    }
    
    @Test
    @DisplayName("Should not record seatmap request for guest when seat map data is empty")
    void guestAccessRecording_EmptySeatMapData_DoesNotRecord() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "192.168.1.100"
        ));
        
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        
        // Mock Amadeus returning empty seat map data
        JsonNode emptySeatMapData = objectMapper.readTree("{\"data\":[]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString())).thenReturn(emptySeatMapData);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest(anyString());
    }
    
    @Test
    @DisplayName("Should not record seatmap request for guest when seat map data is null")
    void guestAccessRecording_NullSeatMapData_DoesNotRecord() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "192.168.1.100"
        ));
        
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        when(mockAmadeusService.getSeatMapFromOfferData(anyString())).thenReturn(null);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest(anyString());
    }
    
    @Test
    @DisplayName("Should not record seatmap request for registered users")
    void guestAccessRecording_RegisteredUser_DoesNotRecord() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        setupValidUserToken();
        setupValidAmadeusResponse();
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest(anyString());
        verify(mockGuestAccessRepository, never()).canMakeSeatmapRequest(anyString());
    }
    
    @Test
    @DisplayName("Should continue request processing even if recording fails")
    void guestAccessRecording_RecordingFails_ContinuesProcessing() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "192.168.1.100"
        ));
        
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100")).thenReturn(true);
        setupValidAmadeusResponse();
        
        // Mock recording failure
        doThrow(new RuntimeException("DynamoDB error")).when(mockGuestAccessRepository).recordSeatmapRequest("192.168.1.100");
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
    }

    // ========== ERROR HANDLING TESTS ==========
    
    @Test
    @DisplayName("Should handle guest access check failure gracefully")
    void errorHandling_GuestAccessCheckFailure() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of(
            "Authorization", "Bearer guest-token",
            "X-Forwarded-For", "192.168.1.100"
        ));
        
        setupValidGuestToken();
        when(mockGuestAccessRepository.canMakeSeatmapRequest("192.168.1.100"))
            .thenThrow(new RuntimeException("DynamoDB connection error"));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Error validating token"));
    }
    
    @Test
    @DisplayName("Should handle response formatting error gracefully")
    void errorHandling_ResponseFormattingError() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        setupValidUserToken();
        
        // Mock Amadeus throwing an exception during processing
        when(mockAmadeusService.getSeatMapFromOfferData(anyString()))
            .thenThrow(new RuntimeException("JSON processing error"));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Internal server error"));
    }
    
    @Test
    @DisplayName("Should handle unexpected exceptions gracefully")
    void errorHandling_UnexpectedException() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        when(mockJwtService.validateToken("user-token")).thenThrow(new RuntimeException("Unexpected error"));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Internal server error"));
    }

    // ========== HELPER METHODS ==========
    
    private APIGatewayProxyRequestEvent createBasicRequest() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        String flightOfferData = createAmadeusFlightOfferData();
        request.setBody(createSeatMapRequestBody(flightOfferData));
        return request;
    }
    
    private APIGatewayProxyRequestEvent createBasicRequestWithContext() {
        APIGatewayProxyRequestEvent request = createBasicRequest();
        APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
        APIGatewayProxyRequestEvent.RequestIdentity identity = new APIGatewayProxyRequestEvent.RequestIdentity();
        context.setIdentity(identity);
        request.setRequestContext(context);
        return request;
    }
    
    private String createSeatMapRequestBody(String flightOfferData) {
        return String.format("{\"flightOfferData\":\"%s\"}", 
            flightOfferData.replace("\"", "\\\""));
    }
    
    private String createAmadeusFlightOfferData() {
        return "{\"id\":\"offer123\",\"dataSource\":\"AMADEUS\",\"type\":\"flight-offer\"}";
    }
    
    private String createSabreFlightOfferData() {
        return "{\"id\":\"offer123\",\"dataSource\":\"SABRE\",\"itineraries\":[{\"segments\":[{" +
               "\"carrierCode\":\"AA\",\"number\":\"123\"," +
               "\"departure\":{\"iataCode\":\"LAX\",\"at\":\"2024-12-01T10:00:00\"}," +
               "\"arrival\":{\"iataCode\":\"JFK\",\"at\":\"2024-12-01T18:00:00\"}" +
               "}]}]}";
    }
    
    private void setupValidUserToken() throws Exception {
        when(mockJwtService.validateToken("user-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("user-token")).thenReturn(false);
    }
    
    private void setupValidGuestToken() throws Exception {
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
    }
    
    private void setupValidAmadeusResponse() throws Exception {
        JsonNode mockSeatMapData = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\"}]}");
        when(mockAmadeusService.getSeatMapFromOfferData(anyString())).thenReturn(mockSeatMapData);
    }
    
    // ========== BOOKMARK SEATMAP TESTS ==========
    
    @Test
    @DisplayName("Should successfully get seatmap by bookmark ID")
    void getSeatMapByBookmark_ValidBookmark_ReturnsSeatMap() throws Exception {
        // Given
        String bookmarkId = "bookmark-123";
        String userId = "user-456";
        String flightOfferData = createAmadeusFlightOfferData();
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/seat-map/bookmark/" + bookmarkId);
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        Bookmark bookmark = createTestBookmark(userId, bookmarkId, flightOfferData);
        
        // Setup mocks
        when(mockJwtService.validateToken("user-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("user-token")).thenReturn(false);
        when(mockClaims.getSubject()).thenReturn(userId);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId))
                .thenReturn(Optional.of(bookmark));
        setupValidAmadeusResponse();
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(userId, bookmarkId);
        verify(mockAmadeusService).getSeatMapFromOfferData(flightOfferData);
    }
    
    @Test
    @DisplayName("Should return 404 when bookmark not found")
    void getSeatMapByBookmark_BookmarkNotFound_Returns404() throws Exception {
        // Given
        String bookmarkId = "nonexistent-bookmark";
        String userId = "user-456";
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/seat-map/bookmark/" + bookmarkId);
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        // Setup mocks
        when(mockJwtService.validateToken("user-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("user-token")).thenReturn(false);
        when(mockClaims.getSubject()).thenReturn(userId);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId))
                .thenReturn(Optional.empty());
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Bookmark not found"));
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(userId, bookmarkId);
    }
    
    @Test
    @DisplayName("Should return 410 when bookmark is expired")
    void getSeatMapByBookmark_ExpiredBookmark_Returns410() throws Exception {
        // Given
        String bookmarkId = "expired-bookmark";
        String userId = "user-456";
        String flightOfferData = createAmadeusFlightOfferData();
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/seat-map/bookmark/" + bookmarkId);
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        Bookmark expiredBookmark = createTestBookmark(userId, bookmarkId, flightOfferData);
        expiredBookmark.setExpiresAt(Instant.now().minusSeconds(3600)); // Expired 1 hour ago
        
        // Setup mocks
        when(mockJwtService.validateToken("user-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("user-token")).thenReturn(false);
        when(mockClaims.getSubject()).thenReturn(userId);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId))
                .thenReturn(Optional.of(expiredBookmark));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(410, response.getStatusCode());
        assertTrue(response.getBody().contains("Bookmark has expired"));
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(userId, bookmarkId);
    }
    
    @Test
    @DisplayName("Should return 401 when guest token is used for bookmark access")
    void getSeatMapByBookmark_GuestToken_Returns401() throws Exception {
        // Given
        String bookmarkId = "bookmark-123";
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/seat-map/bookmark/" + bookmarkId);
        request.setHeaders(Map.of("Authorization", "Bearer guest-token"));
        
        // Setup mocks
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Valid user authentication required"));
        verify(mockJwtService).validateToken("guest-token");
        verify(mockJwtService).isGuestToken("guest-token");
    }
    
    @Test
    @DisplayName("Should return 401 when no authorization header is provided")
    void getSeatMapByBookmark_NoAuthHeader_Returns401() throws Exception {
        // Given
        String bookmarkId = "bookmark-123";
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/seat-map/bookmark/" + bookmarkId);
        request.setHeaders(new HashMap<>()); // No auth header
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Authorization token required"));
    }
    
    @Test
    @DisplayName("Should handle Sabre flight offers in bookmark seatmap")
    void getSeatMapByBookmark_SabreFlightOffer_CallsSabreService() throws Exception {
        // Given
        String bookmarkId = "sabre-bookmark";
        String userId = "user-456";
        String sabreFlightOfferData = createSabreFlightOfferData();
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/seat-map/bookmark/" + bookmarkId);
        request.setHeaders(Map.of("Authorization", "Bearer user-token"));
        
        Bookmark bookmark = createTestBookmark(userId, bookmarkId, sabreFlightOfferData);
        
        // Setup mocks
        when(mockJwtService.validateToken("user-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("user-token")).thenReturn(false);
        when(mockClaims.getSubject()).thenReturn(userId);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId))
                .thenReturn(Optional.of(bookmark));
        
        JsonNode mockSabreResponse = objectMapper.readTree("{\"data\":[{\"seat\":\"1A\",\"source\":\"sabre\"}]}");
        when(mockSabreService.getSeatMapFromFlight("AA", "123", "2024-12-01", "LAX", "JFK"))
                .thenReturn(mockSabreResponse);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(userId, bookmarkId);
        verify(mockSabreService).getSeatMapFromFlight("AA", "123", "2024-12-01", "LAX", "JFK");
    }
    
    @Test
    @DisplayName("Should return 401 when JWT token is invalid")
    void getSeatMapByBookmark_InvalidToken_Returns401() throws Exception {
        // Given
        String bookmarkId = "bookmark-123";
        
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setPath("/seat-map/bookmark/" + bookmarkId);
        request.setHeaders(Map.of("Authorization", "Bearer invalid-token"));
        
        // Setup mocks
        when(mockJwtService.validateToken("invalid-token"))
                .thenThrow(new com.seatmap.common.exception.SeatmapException("TOKEN_INVALID", "Invalid token"));
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
        verify(mockJwtService).validateToken("invalid-token");
    }
    
    private Bookmark createTestBookmark(String userId, String bookmarkId, String flightOfferData) {
        Bookmark bookmark = new Bookmark(userId, bookmarkId, "Test Flight", flightOfferData);
        bookmark.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days from now
        return bookmark;
    }
}