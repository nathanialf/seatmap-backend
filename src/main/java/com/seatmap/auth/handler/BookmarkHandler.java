package com.seatmap.auth.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.auth.model.CreateBookmarkRequest;
import com.seatmap.auth.repository.BookmarkRepository;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.repository.SessionRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.auth.repository.UserUsageRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.email.service.EmailService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.auth.service.PasswordService;
import com.seatmap.auth.service.UserUsageLimitsService;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Bookmark;
import com.seatmap.common.model.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Instant;
import java.util.*;

public class BookmarkHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(BookmarkHandler.class);
    
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final BookmarkRepository bookmarkRepository;
    private final AuthService authService;
    private final UserUsageLimitsService usageLimitsService;
    
    public BookmarkHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        
        // Initialize AWS services
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
        
        // Get environment
        String environment = System.getenv("ENVIRONMENT");
        if (environment == null) environment = "dev";
        
        // Initialize repositories with table names
        String usersTable = "seatmap-users-" + environment;
        String sessionsTable = "seatmap-sessions-" + environment;
        String guestAccessTable = "seatmap-guest-access-" + environment;
        String bookmarksTable = "seatmap-bookmarks-" + environment;
        
        this.bookmarkRepository = new BookmarkRepository(dynamoDbClient, bookmarksTable);
        
        // Initialize AuthService with all dependencies for token validation
        UserRepository userRepository = new UserRepository(dynamoDbClient, usersTable);
        SessionRepository sessionRepository = new SessionRepository(dynamoDbClient, sessionsTable);
        GuestAccessRepository guestAccessRepository = new GuestAccessRepository(dynamoDbClient);
        UserUsageRepository userUsageRepository = new UserUsageRepository(dynamoDbClient);
        PasswordService passwordService = new PasswordService();
        JwtService jwtService = new JwtService();
        EmailService emailService = new EmailService();
        
        this.authService = new AuthService(userRepository, sessionRepository, passwordService, jwtService, guestAccessRepository, userUsageRepository, emailService);
        
        // Initialize UserUsageLimitsService for tier-based limits (reuse the same repository)
        this.usageLimitsService = new UserUsageLimitsService(userUsageRepository, dynamoDbClient);
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            logger.info("Processing bookmark request: {} {}", event.getHttpMethod(), event.getPath());
            
            String httpMethod = event.getHttpMethod();
            String path = event.getPath();
            
            if ("GET".equals(httpMethod) && "/bookmarks".equals(path)) {
                return handleListBookmarks(event);
            } else if ("POST".equals(httpMethod) && "/bookmarks".equals(path)) {
                return handleCreateBookmark(event);
            } else if ("DELETE".equals(httpMethod) && path.startsWith("/bookmarks/")) {
                String bookmarkId = path.substring("/bookmarks/".length());
                if (bookmarkId.contains("/")) {
                    return createErrorResponse(400, "Invalid bookmark ID");
                }
                return handleDeleteBookmark(event, bookmarkId);
            } else if ("GET".equals(httpMethod) && path.startsWith("/bookmarks/")) {
                String pathSuffix = path.substring("/bookmarks/".length());
                if (pathSuffix.contains("/execute")) {
                    String bookmarkId = pathSuffix.replace("/execute", "");
                    return handleExecuteBookmark(event, bookmarkId);
                } else {
                    return handleGetBookmark(event, pathSuffix);
                }
            } else if ("POST".equals(httpMethod) && path.matches("/bookmarks/[^/]+/execute")) {
                String bookmarkId = path.split("/")[2]; // Extract ID from /bookmarks/{id}/execute
                return handleExecuteBookmark(event, bookmarkId);
            }
            
            // Check if path is valid but method is not allowed
            if (path.equals("/bookmarks") || path.startsWith("/bookmarks/")) {
                return createErrorResponse(405, "Method not allowed");
            }
            
            return createErrorResponse(404, "Not found");
        } catch (SeatmapException e) {
            logger.error("Seatmap error in bookmark handler", e);
            return createErrorResponse(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in bookmark handler", e);
            return createErrorResponse(500, "Internal server error");
        }
    }
    
    private APIGatewayProxyResponseEvent handleListBookmarks(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Processing list bookmarks request");
        
        String userId = extractUserIdFromToken(event);
        if (userId == null) {
            return createErrorResponse(401, "Invalid or missing authentication token");
        }
        
        // Check for itemType filter parameter
        String typeFilter = null;
        if (event.getQueryStringParameters() != null) {
            typeFilter = event.getQueryStringParameters().get("type");
        }
        
        List<Bookmark> bookmarks;
        if (typeFilter != null) {
            try {
                Bookmark.ItemType itemType = Bookmark.ItemType.valueOf(typeFilter.toUpperCase());
                bookmarks = bookmarkRepository.findByUserIdAndItemType(userId, itemType);
                logger.info("Filtering bookmarks by type: {}", itemType);
            } catch (IllegalArgumentException e) {
                return createErrorResponse(400, "Invalid item type. Valid types: BOOKMARK, SAVED_SEARCH");
            }
        } else {
            bookmarks = bookmarkRepository.findByUserId(userId);
        }
        
        // Filter out expired bookmarks (they'll be cleaned up by TTL eventually)
        List<Bookmark> activeBookmarks = bookmarks.stream()
                .filter(bookmark -> !bookmark.isExpired())
                .toList();
        
        logger.info("Retrieved {} active bookmarks for user: {}", activeBookmarks.size(), userId);
        
        // Get user for tier information
        User user = authService.validateToken(extractTokenFromEvent(event));
        
        try {
            int remainingBookmarks = usageLimitsService.getRemainingBookmarks(user);
            
            // Determine response key based on filter
            String responseKey = (typeFilter != null && "SAVED_SEARCH".equals(typeFilter.toUpperCase())) 
                ? "savedSearches" : "bookmarks";
            
            return createSuccessResponse(Map.of(
                responseKey, activeBookmarks,
                "total", activeBookmarks.size(),
                "tier", user.getAccountTier(),
                "remainingThisMonth", remainingBookmarks
            ));
        } catch (SeatmapException e) {
            logger.error("Error getting remaining bookmarks for user: {}", user.getUserId(), e);
            return createErrorResponse(e.getHttpStatus(), e.getMessage());
        }
    }
    
    private APIGatewayProxyResponseEvent handleCreateBookmark(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Processing create bookmark request");
        
        // Validate token and get user
        User user = authService.validateToken(extractTokenFromEvent(event));
        if (user == null) {
            return createErrorResponse(401, "Invalid or missing authentication token");
        }
        
        // Parse request body
        CreateBookmarkRequest request;
        try {
            request = objectMapper.readValue(event.getBody(), CreateBookmarkRequest.class);
        } catch (Exception e) {
            logger.error("Error parsing create bookmark request body", e);
            return createErrorResponse(400, "Invalid request format");
        }
        
        // Validate request
        Set<ConstraintViolation<CreateBookmarkRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ConstraintViolation<CreateBookmarkRequest> violation : violations) {
                errors.append(violation.getMessage()).append("; ");
            }
            return createErrorResponse(400, "Validation errors: " + errors.toString());
        }
        
        // Additional validation for item type specific requirements
        if (!request.isValid()) {
            String validationError = request.getValidationError();
            return createErrorResponse(400, validationError != null ? validationError : "Invalid request data for item type");
        }
        
        // Check tier-based bookmark limit and record usage
        try {
            usageLimitsService.recordBookmarkCreation(user);
        } catch (SeatmapException e) {
            return createErrorResponse(e.getHttpStatus(), e.getMessage());
        }
        
        // Create new bookmark based on item type
        String bookmarkId = UUID.randomUUID().toString();
        Bookmark bookmark;
        
        if (request.getItemType() == Bookmark.ItemType.BOOKMARK) {
            bookmark = new Bookmark(user.getUserId(), bookmarkId, request.getTitle(), request.getFlightOfferData());
        } else if (request.getItemType() == Bookmark.ItemType.SAVED_SEARCH) {
            bookmark = new Bookmark(user.getUserId(), bookmarkId, request.getTitle(), request.getSearchRequest());
        } else {
            return createErrorResponse(400, "Invalid item type");
        }
        
        bookmarkRepository.saveBookmark(bookmark);
        
        logger.info("Created {} {} for user: {} tier: {}", 
            request.getItemType().name().toLowerCase(), bookmarkId, user.getUserId(), user.getAccountTier());
        
        return createSuccessResponse(bookmark);
    }
    
    private APIGatewayProxyResponseEvent handleDeleteBookmark(APIGatewayProxyRequestEvent event, String bookmarkId) throws SeatmapException {
        logger.info("Processing delete bookmark request for ID: {}", bookmarkId);
        
        String userId = extractUserIdFromToken(event);
        if (userId == null) {
            return createErrorResponse(401, "Invalid or missing authentication token");
        }
        
        // Check if bookmark exists and belongs to user
        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId);
        if (existingBookmark.isEmpty()) {
            return createErrorResponse(404, "Bookmark not found");
        }
        
        bookmarkRepository.deleteBookmark(userId, bookmarkId);
        
        logger.info("Deleted bookmark {} for user: {}", bookmarkId, userId);
        
        return createSuccessResponse(Map.of("message", "Bookmark deleted successfully"));
    }
    
    private APIGatewayProxyResponseEvent handleGetBookmark(APIGatewayProxyRequestEvent event, String bookmarkId) throws SeatmapException {
        logger.info("Processing get bookmark request for ID: {}", bookmarkId);
        
        String userId = extractUserIdFromToken(event);
        if (userId == null) {
            return createErrorResponse(401, "Invalid or missing authentication token");
        }
        
        Optional<Bookmark> bookmark = bookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId);
        if (bookmark.isEmpty()) {
            return createErrorResponse(404, "Bookmark not found");
        }
        
        if (bookmark.get().isExpired()) {
            return createErrorResponse(410, "Bookmark has expired");
        }
        
        return createSuccessResponse(bookmark.get());
    }
    
    private String extractTokenFromEvent(APIGatewayProxyRequestEvent event) {
        String authHeader = event.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
    
    private APIGatewayProxyResponseEvent handleExecuteBookmark(APIGatewayProxyRequestEvent event, String bookmarkId) throws SeatmapException {
        logger.info("Processing execute bookmark request for ID: {}", bookmarkId);
        
        String userId = extractUserIdFromToken(event);
        if (userId == null) {
            return createErrorResponse(401, "Invalid or missing authentication token");
        }
        
        Optional<Bookmark> bookmark = bookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId);
        if (bookmark.isEmpty()) {
            return createErrorResponse(404, "Bookmark not found");
        }
        
        // Only saved searches can be executed
        if (bookmark.get().getItemType() != Bookmark.ItemType.SAVED_SEARCH) {
            return createErrorResponse(400, "Only saved search items can be executed");
        }
        
        // Update last accessed time
        bookmark.get().updateLastAccessed();
        bookmarkRepository.saveBookmark(bookmark.get());
        
        // Return the search request that can be executed by the frontend
        return createSuccessResponse(Map.of(
            "searchRequest", bookmark.get().getSearchRequest(),
            "title", bookmark.get().getTitle(),
            "searchId", bookmarkId,
            "message", "Search request ready for execution"
        ));
    }
    
    private String extractUserIdFromToken(APIGatewayProxyRequestEvent event) {
        String token = extractTokenFromEvent(event);
        if (token == null) {
            return null;
        }
        
        try {
            // Validate token and get user (guest tokens not allowed for bookmarks)
            User user = authService.validateToken(token);
            if (user == null) {
                return null; // Invalid or guest token
            }
            
            return user.getUserId();
        } catch (Exception e) {
            logger.error("Error validating token", e);
            return null;
        }
    }
    
    private APIGatewayProxyResponseEvent createSuccessResponse(Object data) {
        try {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(createCorsHeaders())
                .withBody(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            logger.error("Error creating success response", e);
            return createErrorResponse(500, "Error creating response");
        }
    }
    
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        
        try {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(createCorsHeaders())
                .withBody(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            logger.error("Error creating error response", e);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(createCorsHeaders())
                .withBody("{\"success\":false,\"message\":\"Internal server error\"}");
        }
    }
    
    private Map<String, String> createCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization, X-API-Key");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}