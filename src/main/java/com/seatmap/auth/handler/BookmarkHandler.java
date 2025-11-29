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
import com.seatmap.auth.model.AlertRequest;
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
            } else if ("PATCH".equals(httpMethod) && path.matches("/bookmarks/[^/]+/alert")) {
                String bookmarkId = path.substring("/bookmarks/".length(), path.lastIndexOf("/alert"));
                return handleCreateOrUpdateAlert(event, bookmarkId);
            } else if ("DELETE".equals(httpMethod) && path.matches("/bookmarks/[^/]+/alert")) {
                String bookmarkId = path.substring("/bookmarks/".length(), path.lastIndexOf("/alert"));
                return handleDeleteAlert(event, bookmarkId);
            } else if ("DELETE".equals(httpMethod) && path.startsWith("/bookmarks/")) {
                String bookmarkId = path.substring("/bookmarks/".length());
                if (bookmarkId.contains("/")) {
                    return createErrorResponse(400, "Invalid bookmark ID");
                }
                return handleDeleteBookmark(event, bookmarkId);
            } else if ("GET".equals(httpMethod) && path.startsWith("/bookmarks/")) {
                String pathSuffix = path.substring("/bookmarks/".length());
                return handleGetBookmark(event, pathSuffix);
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
                "remaining", remainingBookmarks
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
        
        // Additional validation for SAVED_SEARCH items
        if (request.getItemType() == Bookmark.ItemType.SAVED_SEARCH) {
            FlightSearchRequest searchReq = request.getSearchRequest();
            if (searchReq != null && !searchReq.isValid()) {
                return createErrorResponse(400, searchReq.getValidationError());
            }
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
            bookmark = new Bookmark(user.getUserId(), bookmarkId, request.getTitle(), request.getFlightOfferData(), Bookmark.ItemType.BOOKMARK);
        } else if (request.getItemType() == Bookmark.ItemType.SAVED_SEARCH) {
            // Use the new constructor that populates top-level flight search fields
            bookmark = new Bookmark(user.getUserId(), bookmarkId, request.getTitle(), request.getSearchRequest(), Bookmark.ItemType.SAVED_SEARCH);
        } else {
            return createErrorResponse(400, "Invalid item type");
        }
        
        // Set alert configuration if provided
        if (request.getAlertConfig() != null) {
            bookmark.setAlertConfig(request.getAlertConfig());
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
    
    private APIGatewayProxyResponseEvent handleCreateOrUpdateAlert(APIGatewayProxyRequestEvent event, String bookmarkId) throws SeatmapException {
        logger.info("Processing create/update alert request for bookmark ID: {}", bookmarkId);
        
        String userId = extractUserIdFromToken(event);
        if (userId == null) {
            return createErrorResponse(401, "Invalid or missing authentication token");
        }
        
        // Parse request body
        AlertRequest alertRequest;
        try {
            alertRequest = objectMapper.readValue(event.getBody(), AlertRequest.class);
        } catch (Exception e) {
            logger.error("Error parsing alert request body", e);
            return createErrorResponse(400, "Invalid request format");
        }
        
        // Validate request
        Set<ConstraintViolation<AlertRequest>> violations = validator.validate(alertRequest);
        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ConstraintViolation<AlertRequest> violation : violations) {
                errors.append(violation.getMessage()).append("; ");
            }
            return createErrorResponse(400, "Validation errors: " + errors.toString());
        }
        
        // Check if bookmark exists and belongs to user
        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId);
        if (existingBookmark.isEmpty()) {
            return createErrorResponse(404, "Bookmark not found");
        }
        
        if (existingBookmark.get().isExpired()) {
            return createErrorResponse(410, "Bookmark has expired");
        }
        
        // Create or update alert configuration
        Bookmark bookmark = existingBookmark.get();
        Bookmark.AlertConfig alertConfig = new Bookmark.AlertConfig(alertRequest.getAlertThreshold());
        bookmark.setAlertConfig(alertConfig);
        bookmark.updateTimestamp();
        
        bookmarkRepository.saveBookmark(bookmark);
        
        logger.info("Created/updated alert for bookmark {} with threshold: {}", bookmarkId, alertRequest.getAlertThreshold());
        
        return createSuccessResponse(Map.of(
            "message", "Alert configured successfully",
            "alertConfig", alertConfig
        ));
    }
    
    private APIGatewayProxyResponseEvent handleDeleteAlert(APIGatewayProxyRequestEvent event, String bookmarkId) throws SeatmapException {
        logger.info("Processing delete alert request for bookmark ID: {}", bookmarkId);
        
        String userId = extractUserIdFromToken(event);
        if (userId == null) {
            return createErrorResponse(401, "Invalid or missing authentication token");
        }
        
        // Check if bookmark exists and belongs to user
        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId);
        if (existingBookmark.isEmpty()) {
            return createErrorResponse(404, "Bookmark not found");
        }
        
        Bookmark bookmark = existingBookmark.get();
        if (bookmark.getAlertConfig() == null || !bookmark.getAlertConfig().isEnabled()) {
            return createErrorResponse(404, "No alert configured for this bookmark");
        }
        
        // Remove alert configuration
        bookmark.setAlertConfig(null);
        bookmark.updateTimestamp();
        
        bookmarkRepository.saveBookmark(bookmark);
        
        logger.info("Deleted alert for bookmark: {}", bookmarkId);
        
        return createSuccessResponse(Map.of("message", "Alert removed successfully"));
    }
    
    private String extractTokenFromEvent(APIGatewayProxyRequestEvent event) {
        String authHeader = event.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
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
            // Log AlertConfig data before API response serialization
            logApiResponseAlertConfig("BEFORE_API_RESPONSE", data);
            
            String responseBody = objectMapper.writeValueAsString(data);
            
            // Log the actual JSON that will be sent to client
            logApiResponseJson("API_RESPONSE_JSON", responseBody);
            
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(createCorsHeaders())
                .withBody(responseBody);
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
    
    /**
     * Log AlertConfig data in API responses for debugging
     */
    private void logApiResponseAlertConfig(String context, Object data) {
        try {
            if (data == null) {
                logger.debug("[ALERTCONFIG_API_DEBUG] {}: Response data is NULL", context);
                return;
            }
            
            // Check if data is a Bookmark
            if (data instanceof Bookmark) {
                Bookmark bookmark = (Bookmark) data;
                logSingleBookmarkAlertConfig(context, bookmark);
                return;
            }
            
            // Check if data is a List of Bookmarks
            if (data instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> list = (java.util.List<Object>) data;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Bookmark) {
                        logSingleBookmarkAlertConfig(context + "[" + i + "]", (Bookmark) item);
                    }
                }
                return;
            }
            
            // Check if data is a Map containing bookmarks
            if (data instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) data;
                
                // Check for "data" field containing bookmarks
                Object dataField = map.get("data");
                if (dataField instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> list = (java.util.List<Object>) dataField;
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (item instanceof Bookmark) {
                            logSingleBookmarkAlertConfig(context + ".data[" + i + "]", (Bookmark) item);
                        }
                    }
                } else if (dataField instanceof Bookmark) {
                    logSingleBookmarkAlertConfig(context + ".data", (Bookmark) dataField);
                }
            }
            
        } catch (Exception e) {
            logger.warn("[ALERTCONFIG_API_DEBUG] Error logging AlertConfig for {}: {}", context, e.getMessage());
        }
    }
    
    private void logSingleBookmarkAlertConfig(String context, Bookmark bookmark) {
        if (bookmark == null) {
            logger.debug("[ALERTCONFIG_API_DEBUG] {}: Bookmark is NULL", context);
            return;
        }
        
        String bookmarkId = bookmark.getBookmarkId();
        String userId = bookmark.getUserId();
        
        if (bookmark.getAlertConfig() == null) {
            logger.debug("[ALERTCONFIG_API_DEBUG] {}: BookmarkId={}, UserId={}, AlertConfig=NULL", 
                context, bookmarkId, userId);
            return;
        }
        
        Bookmark.AlertConfig alertConfig = bookmark.getAlertConfig();
        
        logger.info("[ALERTCONFIG_API_DEBUG] {}: BookmarkId={}, UserId={}, " +
            "AlertThreshold={}, LastEvaluated={}, LastTriggered={}, TriggerHistory={}", 
            context, bookmarkId, userId,
            alertConfig.getAlertThreshold(),
            alertConfig.getLastEvaluated(),
            alertConfig.getLastTriggered(),
            alertConfig.getTriggerHistory() != null ? "[" + alertConfig.getTriggerHistory().length() + " chars]" : "null"
        );
    }
    
    /**
     * Log the actual JSON response to check for field presence
     */
    private void logApiResponseJson(String context, String responseBody) {
        try {
            if (responseBody == null || responseBody.isEmpty()) {
                logger.debug("[ALERTCONFIG_JSON_DEBUG] {}: Response body is empty", context);
                return;
            }
            
            // Check for AlertConfig fields in the JSON
            boolean hasAlertConfig = responseBody.contains("\"alertConfig\":");
            boolean hasLastEvaluated = responseBody.contains("\"lastEvaluated\":");
            boolean hasLastTriggered = responseBody.contains("\"lastTriggered\":");
            boolean hasAlertThreshold = responseBody.contains("\"alertThreshold\":");
            
            if (hasAlertConfig) {
                logger.info("[ALERTCONFIG_JSON_DEBUG] {}: JSON contains AlertConfig fields - " +
                    "alertThreshold:{}, lastEvaluated:{}, lastTriggered:{}", 
                    context, hasAlertThreshold, hasLastEvaluated, hasLastTriggered);
                    
                // Log a snippet of the alertConfig JSON for inspection
                int alertConfigStart = responseBody.indexOf("\"alertConfig\":");
                if (alertConfigStart != -1) {
                    int alertConfigEnd = findJsonObjectEnd(responseBody, alertConfigStart + 14);
                    if (alertConfigEnd != -1) {
                        String alertConfigJson = responseBody.substring(alertConfigStart, alertConfigEnd + 1);
                        logger.info("[ALERTCONFIG_JSON_DEBUG] {}: AlertConfig JSON snippet: {}", 
                            context, alertConfigJson);
                    }
                }
            } else {
                logger.debug("[ALERTCONFIG_JSON_DEBUG] {}: JSON does not contain AlertConfig", context);
            }
            
        } catch (Exception e) {
            logger.warn("[ALERTCONFIG_JSON_DEBUG] Error analyzing JSON for {}: {}", context, e.getMessage());
        }
    }
    
    /**
     * Find the end of a JSON object starting from a given position
     */
    private int findJsonObjectEnd(String json, int startPos) {
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        return i;
                    }
                }
            }
        }
        
        return -1;
    }
}