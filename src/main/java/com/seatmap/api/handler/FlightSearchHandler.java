package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.api.model.FlightSearchResponse;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.api.service.FlightSearchService;
import com.seatmap.api.service.SabreService;
import com.seatmap.auth.repository.BookmarkRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Bookmark;
import io.jsonwebtoken.Claims;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FlightSearchHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(FlightSearchHandler.class);
    
    private final ObjectMapper objectMapper;
    private final AmadeusService amadeusService;
    private final SabreService sabreService;
    private final FlightSearchService flightSearchService;
    private final JwtService jwtService;
    private final AuthService authService;
    private final BookmarkRepository bookmarkRepository;
    private final Validator validator;
    
    public FlightSearchHandler() {
        this.objectMapper = new ObjectMapper();
        this.amadeusService = new AmadeusService();
        this.sabreService = new SabreService();
        this.flightSearchService = new FlightSearchService(amadeusService, sabreService);
        this.jwtService = new JwtService();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        
        // Initialize DynamoDB client and repositories for bookmark functionality
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
        
        String environment = System.getenv("ENVIRONMENT");
        if (environment == null) environment = "dev";
        
        String bookmarksTable = "seatmap-bookmarks-" + environment;
        this.bookmarkRepository = new BookmarkRepository(dynamoDbClient, bookmarksTable);
        
        // Initialize AuthService (needed for bookmark authentication)
        String usersTable = "seatmap-users-" + environment;
        String sessionsTable = "seatmap-sessions-" + environment;
        String guestAccessTable = "seatmap-guest-access-" + environment;
        
        this.authService = new AuthService(
            new com.seatmap.auth.repository.UserRepository(dynamoDbClient, usersTable),
            new com.seatmap.auth.repository.SessionRepository(dynamoDbClient, sessionsTable),
            new com.seatmap.auth.service.PasswordService(),
            jwtService,
            new com.seatmap.auth.repository.GuestAccessRepository(dynamoDbClient),
            new com.seatmap.auth.repository.UserUsageRepository(dynamoDbClient),
            new com.seatmap.email.service.EmailService()
        );
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        logger.info("Processing flight search request: {} {}", event.getHttpMethod(), event.getPath());
        
        try {
            // Check if this is a bookmark flight search request
            String path = event.getPath();
            if ("GET".equals(event.getHttpMethod()) && path != null && path.matches("/flight-search/bookmark/[^/]+")) {
                String bookmarkId = path.substring("/flight-search/bookmark/".length());
                return handleFlightSearchByBookmark(event, bookmarkId);
            }
            
            // Handle regular flight search request (POST /flight-search)
            // Extract and validate JWT token
            String authHeader = event.getHeaders().get("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return createErrorResponse(401, "Missing or invalid authorization header");
            }
            
            String token = authHeader.substring(7);
            try {
                // Validate token and check if user needs email verification
                String userId = jwtService.getUserIdFromToken(token);
                if (!jwtService.isGuestToken(token)) {
                    // For authenticated users, check email verification through auth service
                    jwtService.validateToken(token);
                }
            } catch (com.seatmap.common.exception.SeatmapException e) {
                return createErrorResponse(401, "Invalid or expired token");
            }
            
            // Parse request body
            FlightSearchRequest request;
            try {
                request = objectMapper.readValue(event.getBody(), FlightSearchRequest.class);
            } catch (Exception e) {
                logger.error("Error parsing request body", e);
                return createErrorResponse(400, "Invalid request format");
            }
            
            // Validate request
            Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder errors = new StringBuilder();
                for (ConstraintViolation<FlightSearchRequest> violation : violations) {
                    errors.append(violation.getMessage()).append("; ");
                }
                return createErrorResponse(400, "Validation errors: " + errors.toString());
            }
            
            // Search for flights with integrated seatmaps from both sources concurrently
            FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
            
            return createSuccessResponse(response);
            
        } catch (Exception e) {
            logger.error("Error processing flight search request", e);
            return createErrorResponse(500, "Internal server error");
        }
    }
    
    
    
    
    private APIGatewayProxyResponseEvent handleFlightSearchByBookmark(APIGatewayProxyRequestEvent event, String bookmarkId) {
        logger.info("Processing flight search request for bookmark ID: {}", bookmarkId);
        
        // Validate JWT token
        String authHeader = event.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return createErrorResponse(401, "Authorization token required");
        }
        
        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtService.validateToken(token);
        } catch (com.seatmap.common.exception.SeatmapException e) {
            return createErrorResponse(401, "Invalid or expired token");
        }
        
        // Extract user ID from token (bookmarks require authenticated users, not guests)
        String userId;
        try {
            // Check if this is a guest token
            if (jwtService.isGuestToken(token)) {
                return createErrorResponse(401, "Valid user authentication required for bookmark access");
            }
            userId = claims.getSubject();
        } catch (Exception e) {
            logger.error("Error extracting user ID from token", e);
            return createErrorResponse(401, "Error validating user access");
        }
        
        // Get the bookmark
        Optional<Bookmark> bookmarkOpt;
        try {
            bookmarkOpt = bookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId);
        } catch (com.seatmap.common.exception.SeatmapException e) {
            logger.error("Error retrieving bookmark", e);
            return createErrorResponse(500, "Error retrieving bookmark");
        }
        
        if (bookmarkOpt.isEmpty()) {
            return createErrorResponse(404, "Bookmark not found");
        }
        
        Bookmark bookmark = bookmarkOpt.get();
        if (bookmark.isExpired()) {
            return createErrorResponse(410, "Bookmark has expired");
        }
        
        try {
            // Reconstruct flight offer from bookmark
            JsonNode flightOffer = objectMapper.readTree(bookmark.getFlightOfferData());
            
            // Determine data source from flight offer structure or default to AMADEUS
            String dataSource = determineDataSource(flightOffer);
            
            // Get fresh seatmap based on data source
            FlightSearchResult result = fetchFlightWithFreshSeatmap(flightOffer, dataSource);
            
            return createSuccessResponse(result);
            
        } catch (Exception e) {
            logger.error("Error processing bookmark flight search request", e);
            return createErrorResponse(500, "Error retrieving flight data");
        }
    }
    
    private FlightSearchResult fetchFlightWithFreshSeatmap(JsonNode flightOffer, String dataSource) throws Exception {
        try {
            if ("AMADEUS".equals(dataSource)) {
                // Use Amadeus service to get fresh seatmap
                JsonNode seatMapResponse = amadeusService.getSeatMapFromOffer(flightOffer);
                com.seatmap.api.model.SeatMapData seatMapData = new com.seatmap.api.model.SeatMapData();
                seatMapData.setSource("AMADEUS");
                return new FlightSearchResult(flightOffer, seatMapData, true, null);
            } else if ("SABRE".equals(dataSource)) {
                // Extract flight details and use Sabre service
                String carrierCode = extractFromFlightOffer(flightOffer, "carrierCode");
                String flightNumber = extractFromFlightOffer(flightOffer, "number");
                String departureDate = extractFromFlightOffer(flightOffer, "departureDate");
                String origin = extractFromFlightOffer(flightOffer, "origin");
                String destination = extractFromFlightOffer(flightOffer, "destination");
                
                JsonNode seatMapResponse = sabreService.getSeatMapFromFlight(carrierCode, flightNumber, departureDate, origin, destination);
                com.seatmap.api.model.SeatMapData seatMapData = new com.seatmap.api.model.SeatMapData();
                seatMapData.setSource("SABRE");
                return new FlightSearchResult(flightOffer, seatMapData, true, null);
            } else {
                throw new Exception("Unknown data source: " + dataSource);
            }
        } catch (Exception e) {
            // Since this flight was previously bookmarked with working seatmap,
            // failure now indicates temporary API issues (not permanent unsupported flight)
            logger.error("Seatmap temporarily unavailable for bookmarked flight: {}", e.getMessage());
            throw new com.seatmap.common.exception.SeatmapException(
                "SEATMAP_TEMPORARILY_UNAVAILABLE", 
                "Seatmap is temporarily unavailable. Please try again later.", 
                500, 
                e
            );
        }
    }
    
    private String determineDataSource(JsonNode flightOffer) {
        try {
            return flightOffer.path("dataSource").asText("AMADEUS"); // Default to AMADEUS if not found
        } catch (Exception e) {
            logger.warn("Error extracting dataSource from flight offer, defaulting to AMADEUS", e);
            return "AMADEUS";
        }
    }
    
    private String extractFromFlightOffer(JsonNode flightOffer, String field) {
        if (flightOffer.has("itineraries") && flightOffer.get("itineraries").isArray() && flightOffer.get("itineraries").size() > 0) {
            JsonNode firstItinerary = flightOffer.get("itineraries").get(0);
            if (firstItinerary.has("segments") && firstItinerary.get("segments").isArray() && firstItinerary.get("segments").size() > 0) {
                JsonNode firstSegment = firstItinerary.get("segments").get(0);
                
                switch (field) {
                    case "carrierCode":
                        return firstSegment.path("carrierCode").asText("");
                    case "number":
                        return firstSegment.path("number").asText("");
                    case "origin":
                        return firstSegment.path("departure").path("iataCode").asText("");
                    case "destination":
                        return firstSegment.path("arrival").path("iataCode").asText("");
                    case "departureDate":
                        String departureAt = firstSegment.path("departure").path("at").asText("");
                        return departureAt.length() >= 10 ? departureAt.substring(0, 10) : "";
                }
            }
        }
        return "";
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
                .withBody("{\"success\":false,\"message\":\"Internal error\"}");
        }
    }
    
    private Map<String, String> createCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,Authorization,X-API-Key");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}