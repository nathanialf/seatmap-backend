package com.seatmap.auth.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.auth.model.CreateBookmarkRequest;
import com.seatmap.auth.repository.BookmarkRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.auth.service.UserUsageLimitsService;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User.AccountTier;
import com.seatmap.common.model.Bookmark;
import com.seatmap.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkHandlerTest {
    
    private BookmarkHandler handler;
    
    @Mock
    private AuthService mockAuthService;
    
    @Mock
    private BookmarkRepository mockBookmarkRepository;
    
    @Mock
    private UserUsageLimitsService mockUsageLimitsService;
    
    @Mock
    private Context mockContext;
    
    private ObjectMapper objectMapper;
    private final String testUserId = "test-user-123";
    private final String testBookmarkId = "test-bookmark-456";
    
    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new BookmarkHandler();
        
        // Use reflection to inject mocks
        Field authServiceField = BookmarkHandler.class.getDeclaredField("authService");
        authServiceField.setAccessible(true);
        authServiceField.set(handler, mockAuthService);
        
        Field bookmarkRepositoryField = BookmarkHandler.class.getDeclaredField("bookmarkRepository");
        bookmarkRepositoryField.setAccessible(true);
        bookmarkRepositoryField.set(handler, mockBookmarkRepository);
        
        Field usageLimitsServiceField = BookmarkHandler.class.getDeclaredField("usageLimitsService");
        usageLimitsServiceField.setAccessible(true);
        usageLimitsServiceField.set(handler, mockUsageLimitsService);
    }
    
    private APIGatewayProxyRequestEvent createRequestEvent(String method, String path, String authToken, String body) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod(method);
        event.setPath(path);
        
        Map<String, String> headers = new HashMap<>();
        if (authToken != null) {
            headers.put("Authorization", "Bearer " + authToken);
        }
        event.setHeaders(headers);
        
        if (body != null) {
            event.setBody(body);
        }
        
        return event;
    }
    
    private User createTestUser() {
        return createTestUser(AccountTier.PRO);
    }
    
    private User createTestUser(AccountTier tier) {
        User user = new User();
        user.setUserId(testUserId);
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAccountTier(tier);
        return user;
    }
    
    private Bookmark createTestBookmark() throws Exception {
        Map<String, Object> flightOfferMap = new HashMap<>();
        flightOfferMap.put("flight", "AA123");
        flightOfferMap.put("departure", "2024-01-15T10:00:00Z");
        
        String flightOfferData = objectMapper.writeValueAsString(flightOfferMap);
        
        Bookmark bookmark = new Bookmark(testUserId, testBookmarkId, "Test Flight", flightOfferData, Bookmark.ItemType.BOOKMARK);
        bookmark.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days
        return bookmark;
    }
    
    @Test
    void testListBookmarks_ValidToken_ReturnsBookmarks() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks", "valid-token", null);
        User testUser = createTestUser();
        List<Bookmark> bookmarks = Arrays.asList(createTestBookmark());
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserId(testUserId)).thenReturn(bookmarks);
        when(mockUsageLimitsService.getRemainingBookmarks(testUser)).thenReturn(9);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"total\":1"));
        assertTrue(response.getBody().contains("\"tier\":\"PRO\""));
        assertTrue(response.getBody().contains("\"remaining\":9"));
        verify(mockAuthService, times(2)).validateToken("valid-token"); // Called twice in updated handler
        verify(mockBookmarkRepository).findByUserId(testUserId);
        verify(mockUsageLimitsService).getRemainingBookmarks(testUser);
    }
    
    @Test
    void testListBookmarks_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks", "invalid-token", null);
        
        when(mockAuthService.validateToken("invalid-token")).thenReturn(null);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or missing authentication token"));
        verify(mockAuthService).validateToken("invalid-token");
        verify(mockBookmarkRepository, never()).findByUserId(anyString());
    }
    
    @Test
    void testListBookmarks_NoAuthHeader_ReturnsUnauthorized() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks", null, null);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or missing authentication token"));
        verify(mockAuthService, never()).validateToken(anyString());
        verify(mockBookmarkRepository, never()).findByUserId(anyString());
    }
    
    @Test
    void testCreateBookmark_ValidRequest_CreatesBookmark() throws Exception {
        // Arrange
        CreateBookmarkRequest request = new CreateBookmarkRequest();
        request.setTitle("My Test Flight");
        request.setItemType(Bookmark.ItemType.BOOKMARK);
        Map<String, Object> flightData = new HashMap<>();
        flightData.put("flight", "AA123");
        flightData.put("dataSource", "AMADEUS");
        String flightOfferDataString = objectMapper.writeValueAsString(flightData);
        request.setFlightOfferData(flightOfferDataString);
        
        String requestBody = objectMapper.writeValueAsString(request);
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", requestBody);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        doNothing().when(mockUsageLimitsService).recordBookmarkCreation(testUser);
        doNothing().when(mockBookmarkRepository).saveBookmark(any(Bookmark.class));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("My Test Flight"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockUsageLimitsService).recordBookmarkCreation(testUser);
        verify(mockBookmarkRepository).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testCreateBookmark_ExceedsLimit_ReturnsForbidden() throws Exception {
        // Arrange
        CreateBookmarkRequest request = new CreateBookmarkRequest();
        request.setTitle("My Test Flight");
        request.setItemType(Bookmark.ItemType.BOOKMARK);
        request.setFlightOfferData("{\"dataSource\":\"SABRE\"}");
        
        String requestBody = objectMapper.writeValueAsString(request);
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", requestBody);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        doThrow(SeatmapException.forbidden("Monthly bookmark limit reached (10/10) for PRO tier"))
            .when(mockUsageLimitsService).recordBookmarkCreation(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(403, response.getStatusCode());
        assertTrue(response.getBody().contains("Monthly bookmark limit reached"));
        assertTrue(response.getBody().contains("PRO tier"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockUsageLimitsService).recordBookmarkCreation(testUser);
        verify(mockBookmarkRepository, never()).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testCreateBookmark_InvalidRequestBody_ReturnsBadRequest() throws Exception {
        // Arrange
        String invalidJson = "{invalid json}";
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", invalidJson);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository, never()).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testDeleteBookmark_ValidRequest_DeletesBookmark() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("DELETE", "/bookmarks/" + testBookmarkId, "valid-token", null);
        User testUser = createTestUser();
        Bookmark existingBookmark = createTestBookmark();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(testUserId, testBookmarkId))
            .thenReturn(Optional.of(existingBookmark));
        doNothing().when(mockBookmarkRepository).deleteBookmark(testUserId, testBookmarkId);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Bookmark deleted successfully"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(testUserId, testBookmarkId);
        verify(mockBookmarkRepository).deleteBookmark(testUserId, testBookmarkId);
    }
    
    @Test
    void testDeleteBookmark_BookmarkNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("DELETE", "/bookmarks/" + testBookmarkId, "valid-token", null);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(testUserId, testBookmarkId))
            .thenReturn(Optional.empty());
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Bookmark not found"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(testUserId, testBookmarkId);
        verify(mockBookmarkRepository, never()).deleteBookmark(anyString(), anyString());
    }
    
    @Test
    void testGetBookmark_ValidRequest_ReturnsBookmark() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks/" + testBookmarkId, "valid-token", null);
        User testUser = createTestUser();
        Bookmark bookmark = createTestBookmark();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(testUserId, testBookmarkId))
            .thenReturn(Optional.of(bookmark));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Test Flight"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(testUserId, testBookmarkId);
    }
    
    @Test
    void testGetBookmark_ExpiredBookmark_ReturnsGone() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks/" + testBookmarkId, "valid-token", null);
        User testUser = createTestUser();
        Bookmark expiredBookmark = createTestBookmark();
        expiredBookmark.setExpiresAt(Instant.now().minusSeconds(3600)); // Expired 1 hour ago
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(testUserId, testBookmarkId))
            .thenReturn(Optional.of(expiredBookmark));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(410, response.getStatusCode());
        assertTrue(response.getBody().contains("Bookmark has expired"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(testUserId, testBookmarkId);
    }
    
    @Test
    void testMethodNotAllowed_ReturnsMethodNotAllowed() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("PATCH", "/bookmarks", "valid-token", null);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(405, response.getStatusCode());
        assertTrue(response.getBody().contains("Method not allowed"));
        verify(mockAuthService, never()).validateToken(anyString());
    }
    
    @Test
    void testCreateBookmark_FreeUser_ReturnsForbidden() throws Exception {
        // Arrange
        CreateBookmarkRequest request = new CreateBookmarkRequest();
        request.setTitle("My Test Flight");
        request.setItemType(Bookmark.ItemType.BOOKMARK);
        request.setFlightOfferData("{\"dataSource\":\"AMADEUS\"}");
        
        String requestBody = objectMapper.writeValueAsString(request);
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", requestBody);
        User freeUser = createTestUser(AccountTier.FREE);
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(freeUser);
        doThrow(SeatmapException.forbidden("Bookmark creation is not available for FREE tier. Upgrade to PRO or BUSINESS for bookmark access."))
            .when(mockUsageLimitsService).recordBookmarkCreation(freeUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(403, response.getStatusCode());
        assertTrue(response.getBody().contains("Bookmark creation is not available for FREE tier"));
        assertTrue(response.getBody().contains("Upgrade to PRO or BUSINESS"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockUsageLimitsService).recordBookmarkCreation(freeUser);
        verify(mockBookmarkRepository, never()).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testCreateBookmark_BusinessUser_Unlimited_ShouldSucceed() throws Exception {
        // Arrange
        CreateBookmarkRequest request = new CreateBookmarkRequest();
        request.setTitle("Business Flight");
        request.setItemType(Bookmark.ItemType.BOOKMARK);
        request.setFlightOfferData("{\"dataSource\":\"AMADEUS\"}");
        
        String requestBody = objectMapper.writeValueAsString(request);
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", requestBody);
        User businessUser = createTestUser(AccountTier.BUSINESS);
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(businessUser);
        doNothing().when(mockUsageLimitsService).recordBookmarkCreation(businessUser);
        doNothing().when(mockBookmarkRepository).saveBookmark(any(Bookmark.class));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Business Flight"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockUsageLimitsService).recordBookmarkCreation(businessUser);
        verify(mockBookmarkRepository).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testListBookmarks_BusinessUser_ShowsUnlimited() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks", "valid-token", null);
        User businessUser = createTestUser(AccountTier.BUSINESS);
        List<Bookmark> bookmarks = Arrays.asList(createTestBookmark());
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(businessUser);
        when(mockBookmarkRepository.findByUserId(testUserId)).thenReturn(bookmarks);
        when(mockUsageLimitsService.getRemainingBookmarks(businessUser)).thenReturn(Integer.MAX_VALUE);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"total\":1"));
        assertTrue(response.getBody().contains("\"tier\":\"BUSINESS\""));
        assertTrue(response.getBody().contains("\"remaining\":" + Integer.MAX_VALUE));
        verify(mockAuthService, times(2)).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserId(testUserId);
        verify(mockUsageLimitsService).getRemainingBookmarks(businessUser);
    }
    
    @Test
    void testCreateBookmark_ValidationErrors_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateBookmarkRequest request = new CreateBookmarkRequest();
        // Missing title, flightOfferData, and itemType to trigger validation errors
        
        String requestBody = objectMapper.writeValueAsString(request);
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", requestBody);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Validation errors"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository, never()).saveBookmark(any(Bookmark.class));
    }
    
    // SAVED SEARCH TESTS
    
    @Test
    void testCreateSavedSearch_ValidRequest_CreatesBookmark() throws Exception {
        // Arrange
        FlightSearchRequest searchRequest = new FlightSearchRequest();
        searchRequest.setOrigin("LAX");
        searchRequest.setDestination("JFK");
        searchRequest.setDepartureDate("2024-06-15");
        
        CreateBookmarkRequest request = new CreateBookmarkRequest();
        request.setTitle("My Saved Search");
        request.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        request.setSearchRequest(searchRequest);
        
        String requestBody = objectMapper.writeValueAsString(request);
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", requestBody);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        doNothing().when(mockUsageLimitsService).recordBookmarkCreation(testUser);
        doNothing().when(mockBookmarkRepository).saveBookmark(any(Bookmark.class));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("My Saved Search"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockUsageLimitsService).recordBookmarkCreation(testUser);
        verify(mockBookmarkRepository).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testCreateSavedSearch_MissingSearchRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateBookmarkRequest request = new CreateBookmarkRequest();
        request.setTitle("My Saved Search");
        request.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        // Missing searchRequest for saved search
        
        String requestBody = objectMapper.writeValueAsString(request);
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", requestBody);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Search request is required for saved search items"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository, never()).saveBookmark(any(Bookmark.class));
    }
    
    @Test
    void testCreateBookmark_MissingFlightOfferData_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateBookmarkRequest request = new CreateBookmarkRequest();
        request.setTitle("My Bookmark");
        request.setItemType(Bookmark.ItemType.BOOKMARK);
        // Missing flightOfferData for bookmark
        
        String requestBody = objectMapper.writeValueAsString(request);
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks", "valid-token", requestBody);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Flight offer data is required for bookmark items"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository, never()).saveBookmark(any(Bookmark.class));
    }
    
    // TYPE FILTERING TESTS
    
    @Test
    void testListBookmarks_FilterByBookmarkType_ReturnsFilteredResults() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks", "valid-token", null);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("type", "BOOKMARK");
        event.setQueryStringParameters(queryParams);
        
        User testUser = createTestUser();
        List<Bookmark> filteredBookmarks = Arrays.asList(createTestBookmark());
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndItemType(testUserId, Bookmark.ItemType.BOOKMARK))
            .thenReturn(filteredBookmarks);
        when(mockUsageLimitsService.getRemainingBookmarks(testUser)).thenReturn(9);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"total\":1"));
        verify(mockAuthService, times(2)).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndItemType(testUserId, Bookmark.ItemType.BOOKMARK);
        verify(mockUsageLimitsService).getRemainingBookmarks(testUser);
    }
    
    @Test
    void testListBookmarks_FilterBySavedSearchType_ReturnsFilteredResults() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks", "valid-token", null);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("type", "SAVED_SEARCH");
        event.setQueryStringParameters(queryParams);
        
        User testUser = createTestUser();
        List<Bookmark> savedSearches = new ArrayList<>();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndItemType(testUserId, Bookmark.ItemType.SAVED_SEARCH))
            .thenReturn(savedSearches);
        when(mockUsageLimitsService.getRemainingBookmarks(testUser)).thenReturn(9);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"total\":0"));
        verify(mockAuthService, times(2)).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndItemType(testUserId, Bookmark.ItemType.SAVED_SEARCH);
        verify(mockUsageLimitsService).getRemainingBookmarks(testUser);
    }
    
    @Test
    void testListBookmarks_InvalidFilterType_ReturnsBadRequest() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("GET", "/bookmarks", "valid-token", null);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("type", "INVALID_TYPE");
        event.setQueryStringParameters(queryParams);
        
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid item type. Valid types: BOOKMARK, SAVED_SEARCH"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository, never()).findByUserIdAndItemType(anyString(), any());
    }
    
    // SAVED SEARCH EXECUTION TESTS
    
    private Bookmark createTestSavedSearch() throws Exception {
        String searchRequestJson = "{\"origin\":\"LAX\",\"destination\":\"JFK\",\"departureDate\":\"2024-06-15\",\"travelClass\":\"ECONOMY\",\"maxResults\":10}";
        
        Bookmark savedSearch = new Bookmark(testUserId, testBookmarkId, "Test Saved Search", searchRequestJson, Bookmark.ItemType.SAVED_SEARCH);
        savedSearch.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days
        return savedSearch;
    }
    
    @Test
    void testExecuteBookmark_ValidSavedSearch_ReturnsExecutionResult() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks/" + testBookmarkId + "/execute", "valid-token", null);
        User testUser = createTestUser();
        Bookmark savedSearch = createTestSavedSearch();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(testUserId, testBookmarkId))
            .thenReturn(Optional.of(savedSearch));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("message"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(testUserId, testBookmarkId);
    }
    
    @Test
    void testExecuteBookmark_RegularBookmark_ReturnsBadRequest() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks/" + testBookmarkId + "/execute", "valid-token", null);
        User testUser = createTestUser();
        Bookmark regularBookmark = createTestBookmark(); // This is a regular bookmark, not saved search
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(testUserId, testBookmarkId))
            .thenReturn(Optional.of(regularBookmark));
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Only saved search items can be executed"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(testUserId, testBookmarkId);
    }
    
    @Test
    void testExecuteBookmark_NotFound_ReturnsNotFound() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequestEvent("POST", "/bookmarks/" + testBookmarkId + "/execute", "valid-token", null);
        User testUser = createTestUser();
        
        when(mockAuthService.validateToken("valid-token")).thenReturn(testUser);
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(testUserId, testBookmarkId))
            .thenReturn(Optional.empty());
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("Bookmark not found"));
        verify(mockAuthService).validateToken("valid-token");
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(testUserId, testBookmarkId);
    }
}