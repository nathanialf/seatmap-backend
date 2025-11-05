package com.seatmap.auth.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.auth.model.CreateSavedSearchRequest;
import com.seatmap.auth.repository.BookmarkRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.auth.service.UserUsageLimitsService;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Bookmark;
import com.seatmap.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SavedSearchHandlerTest {

    @Mock
    private BookmarkRepository mockBookmarkRepository;
    
    @Mock
    private AuthService mockAuthService;
    
    @Mock
    private UserUsageLimitsService mockUsageLimitsService;
    
    @Mock
    private Context mockContext;
    
    private BookmarkHandler handler;
    private ObjectMapper objectMapper;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        handler = new BookmarkHandler();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Set up test user
        testUser = new User();
        testUser.setUserId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setAccountTier(User.AccountTier.PRO);
        testUser.setEmailVerified(true);
        testUser.setStatus(User.UserStatus.ACTIVE);
        
        // Use reflection to inject mocks
        try {
            var bookmarkRepoField = BookmarkHandler.class.getDeclaredField("bookmarkRepository");
            bookmarkRepoField.setAccessible(true);
            bookmarkRepoField.set(handler, mockBookmarkRepository);
            
            var authServiceField = BookmarkHandler.class.getDeclaredField("authService");
            authServiceField.setAccessible(true);
            authServiceField.set(handler, mockAuthService);
            
            var usageLimitsField = BookmarkHandler.class.getDeclaredField("usageLimitsService");
            usageLimitsField.setAccessible(true);
            usageLimitsField.set(handler, mockUsageLimitsService);
        } catch (Exception e) {
            fail("Failed to inject mocks: " + e.getMessage());
        }
    }
    
    @Test
    void testListSavedSearches_ValidRequest_ReturnsSuccess() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/saved-searches");
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        Bookmark savedSearch = createTestSavedSearch();
        List<Bookmark> savedSearches = Arrays.asList(savedSearch);
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findSavedSearchesByUserId("test-user-id")).thenReturn(savedSearches);
        when(mockUsageLimitsService.getRemainingBookmarks(testUser)).thenReturn(8);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("savedSearches"));
        assertTrue(response.getBody().contains("total"));
        assertTrue(response.getBody().contains("remainingThisMonth"));
        
        verify(mockBookmarkRepository).findSavedSearchesByUserId("test-user-id");
        verify(mockUsageLimitsService).getRemainingBookmarks(testUser);
    }
    
    @Test
    void testListSavedSearches_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/saved-searches");
        event.setHeaders(Map.of("Authorization", "Bearer invalid-token"));
        
        when(mockAuthService.validateToken("invalid-token")).thenReturn(null);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or missing authentication token"));
    }
    
    @Test
    void testCreateSavedSearch_ValidRequest_ReturnsSuccess() throws Exception {
        // Arrange
        CreateSavedSearchRequest request = new CreateSavedSearchRequest();
        request.setTitle("Test Search");
        request.setSearchRequest(createTestFlightSearchRequest());
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/saved-searches");
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        event.setBody(objectMapper.writeValueAsString(request));
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        doNothing().when(mockUsageLimitsService).recordBookmarkCreation(testUser);
        doNothing().when(mockBookmarkRepository).saveBookmark(any(Bookmark.class));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Test Search"));
        assertTrue(response.getBody().contains("SAVED_SEARCH"));
        
        verify(mockUsageLimitsService).recordBookmarkCreation(testUser);
        verify(mockBookmarkRepository).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testCreateSavedSearch_ExceedsLimit_ReturnsForbidden() throws Exception {
        // Arrange
        CreateSavedSearchRequest request = new CreateSavedSearchRequest();
        request.setTitle("Test Search");
        request.setSearchRequest(createTestFlightSearchRequest());
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/saved-searches");
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        event.setBody(objectMapper.writeValueAsString(request));
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        doThrow(SeatmapException.forbidden("Monthly bookmark limit reached (50/50) for PRO tier"))
            .when(mockUsageLimitsService).recordBookmarkCreation(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(403, response.getStatusCode());
        assertTrue(response.getBody().contains("Monthly bookmark limit reached"));
        
        verify(mockUsageLimitsService).recordBookmarkCreation(testUser);
        verify(mockBookmarkRepository, never()).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testCreateSavedSearch_InvalidRequestBody_ReturnsBadRequest() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/saved-searches");
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        event.setBody("{invalid json}");
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
    }
    
    @Test
    void testCreateSavedSearch_ValidationErrors_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateSavedSearchRequest request = new CreateSavedSearchRequest();
        // Missing title and searchRequest - should fail validation
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/saved-searches");
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        event.setBody(objectMapper.writeValueAsString(request));
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Validation errors"));
    }
    
    @Test
    void testGetSavedSearch_ValidRequest_ReturnsSuccess() throws Exception {
        // Arrange
        String searchId = "test-search-id";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/saved-searches/" + searchId);
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        Bookmark savedSearch = createTestSavedSearch();
        savedSearch.setBookmarkId(searchId);
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId("test-user-id", searchId))
            .thenReturn(Optional.of(savedSearch));
        doNothing().when(mockBookmarkRepository).saveBookmark(savedSearch);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains(searchId));
        assertTrue(response.getBody().contains("SAVED_SEARCH"));
        
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId("test-user-id", searchId);
        verify(mockBookmarkRepository).saveBookmark(savedSearch); // lastAccessedAt update
    }
    
    @Test
    void testGetSavedSearch_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String searchId = "non-existent-id";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/saved-searches/" + searchId);
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId("test-user-id", searchId))
            .thenReturn(Optional.empty());
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Saved search not found"));
    }
    
    @Test
    void testGetSavedSearch_IsBookmark_ReturnsNotFound() throws Exception {
        // Arrange
        String searchId = "bookmark-id";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/saved-searches/" + searchId);
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        Bookmark bookmark = createTestBookmark(); // Regular bookmark, not saved search
        bookmark.setBookmarkId(searchId);
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId("test-user-id", searchId))
            .thenReturn(Optional.of(bookmark));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Saved search not found"));
    }
    
    @Test
    void testDeleteSavedSearch_ValidRequest_ReturnsSuccess() throws Exception {
        // Arrange
        String searchId = "test-search-id";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("DELETE");
        event.setPath("/saved-searches/" + searchId);
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        Bookmark savedSearch = createTestSavedSearch();
        savedSearch.setBookmarkId(searchId);
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId("test-user-id", searchId))
            .thenReturn(Optional.of(savedSearch));
        doNothing().when(mockBookmarkRepository).deleteBookmark("test-user-id", searchId);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Saved search deleted successfully"));
        
        verify(mockBookmarkRepository).deleteBookmark("test-user-id", searchId);
    }
    
    @Test
    void testExecuteSavedSearch_ValidRequest_ReturnsSearchRequest() throws Exception {
        // Arrange
        String searchId = "test-search-id";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/saved-searches/" + searchId + "/execute");
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        Bookmark savedSearch = createTestSavedSearch();
        savedSearch.setBookmarkId(searchId);
        savedSearch.setTitle("Test Search");
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId("test-user-id", searchId))
            .thenReturn(Optional.of(savedSearch));
        doNothing().when(mockBookmarkRepository).saveBookmark(savedSearch);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("searchRequest"));
        assertTrue(response.getBody().contains("Test Search"));
        assertTrue(response.getBody().contains("SFO"));
        assertTrue(response.getBody().contains("LAX"));
        
        verify(mockBookmarkRepository).saveBookmark(savedSearch); // lastAccessedAt update
    }
    
    @Test
    void testExecuteSavedSearch_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        String searchId = "non-existent-id";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/saved-searches/" + searchId + "/execute");
        event.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId("test-user-id", searchId))
            .thenReturn(Optional.empty());
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Saved search not found"));
    }
    
    @Test
    void testSavedSearchExpiration_BasedOnDepartureDate() throws Exception {
        // Test that saved searches expire based on departure date, not 30-day TTL
        FlightSearchRequest futureSearchRequest = createTestFlightSearchRequest();
        futureSearchRequest.setDepartureDate("2030-06-15"); // Far future date
        
        FlightSearchRequest pastSearchRequest = createTestFlightSearchRequest();
        pastSearchRequest.setDepartureDate("2023-01-15"); // Past date
        
        // Test future date - should not be expired
        Bookmark futureSearch = new Bookmark("user1", "search1", "Future Search", futureSearchRequest);
        assertFalse(futureSearch.isExpired(), "Saved search with future departure date should not be expired");
        assertNotNull(futureSearch.getExpiresAt(), "Future search should have expiration date set");
        
        // Test past date - should be expired
        Bookmark pastSearch = new Bookmark("user1", "search2", "Past Search", pastSearchRequest);
        assertTrue(pastSearch.isExpired(), "Saved search with past departure date should be expired");
        assertNotNull(pastSearch.getExpiresAt(), "Past search should have expiration date set");
        
        // Verify expiration is set to end of departure day (23:59:59 UTC)
        java.time.LocalDate departureDate = java.time.LocalDate.parse(futureSearchRequest.getDepartureDate());
        java.time.Instant expectedExpiration = departureDate.atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant();
        assertEquals(expectedExpiration, futureSearch.getExpiresAt(), "Expiration should be set to end of departure day");
    }
    
    @Test
    void testSavedSearchEndpoints_AllResponsesHaveCorsHeaders() throws Exception {
        // Test that all saved search endpoints return proper CORS headers
        String searchId = "test-search-id";
        
        // Test list saved searches
        APIGatewayProxyRequestEvent listEvent = new APIGatewayProxyRequestEvent();
        listEvent.setHttpMethod("GET");
        listEvent.setPath("/saved-searches");
        listEvent.setHeaders(Map.of("Authorization", "Bearer valid-token"));
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findSavedSearchesByUserId("test-user-id")).thenReturn(Collections.emptyList());
        when(mockUsageLimitsService.getRemainingBookmarks(testUser)).thenReturn(10);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(listEvent, mockContext);
        
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, POST, PUT, DELETE, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, Authorization, X-API-Key", headers.get("Access-Control-Allow-Headers"));
        assertEquals("application/json", headers.get("Content-Type"));
    }
    
    private Bookmark createTestSavedSearch() {
        Bookmark savedSearch = new Bookmark();
        savedSearch.setUserId("test-user-id");
        savedSearch.setBookmarkId("test-search-id");
        savedSearch.setTitle("Test Saved Search");
        savedSearch.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        savedSearch.setSearchRequest(createTestFlightSearchRequest());
        savedSearch.setCreatedAt(Instant.now());
        savedSearch.setUpdatedAt(Instant.now());
        savedSearch.setLastAccessedAt(Instant.now());
        return savedSearch;
    }
    
    private Bookmark createTestBookmark() {
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user-id");
        bookmark.setBookmarkId("test-bookmark-id");
        bookmark.setTitle("Test Bookmark");
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setFlightOfferData("{\"test\": \"flight offer data\"}");
        bookmark.setCreatedAt(Instant.now());
        bookmark.setUpdatedAt(Instant.now());
        return bookmark;
    }
    
    private FlightSearchRequest createTestFlightSearchRequest() {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setOrigin("SFO");
        request.setDestination("LAX");
        request.setDepartureDate("2024-12-15");
        request.setTravelClass("ECONOMY");
        return request;
    }
}