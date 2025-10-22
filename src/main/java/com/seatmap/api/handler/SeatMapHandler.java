package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapException;
import io.jsonwebtoken.Claims;
import com.seatmap.api.model.SeatMapRequest;
import com.seatmap.api.model.SeatMapResponse;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.api.service.SabreService;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SeatMapHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SeatMapHandler.class);
    
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AmadeusService amadeusService;
    private final SabreService sabreService;
    private final JwtService jwtService;
    private final GuestAccessRepository guestAccessRepository;
    
    public SeatMapHandler() {
        this.objectMapper = new ObjectMapper();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.amadeusService = new AmadeusService();
        this.sabreService = new SabreService();
        this.jwtService = new JwtService();
        
        // Initialize guest access repository with explicit HTTP client to avoid conflicts
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
        this.guestAccessRepository = new GuestAccessRepository(dynamoDbClient);
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        logger.info("Processing seat map request");
        
        try {
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
            
            // For guest users, check IP-based seatmap limits
            String clientIp = extractClientIp(event);
            try {
                if (jwtService.isGuestToken(token)) {
                    // Check if this IP can make seatmap requests
                    if (!guestAccessRepository.canMakeSeatmapRequest(clientIp)) {
                        String denialMessage = guestAccessRepository.getSeatmapDenialMessage(clientIp);
                        logger.warn("Seatmap request denied for guest IP {}: {}", clientIp, denialMessage);
                        return createErrorResponse(403, denialMessage);
                    }
                }
            } catch (com.seatmap.common.exception.SeatmapException e) {
                return createErrorResponse(401, "Error validating token");
            } catch (Exception e) {
                logger.error("Error checking guest access limits for IP: {}", clientIp, e);
                return createErrorResponse(401, "Error validating token");
            }
            
            // Parse request body
            SeatMapRequest request;
            try {
                request = objectMapper.readValue(event.getBody(), SeatMapRequest.class);
            } catch (Exception e) {
                logger.error("Error parsing request body", e);
                return createErrorResponse(400, "Invalid request format");
            }
            
            // Validate request
            Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder errors = new StringBuilder();
                for (ConstraintViolation<SeatMapRequest> violation : violations) {
                    errors.append(violation.getMessage()).append("; ");
                }
                return createErrorResponse(400, "Validation errors: " + errors.toString());
            }
            
            // Route seat map request based on flight source
            JsonNode seatMapData = getSeatMapBySource(request);
            
            // Create successful response
            SeatMapResponse response = SeatMapResponse.success(
                seatMapData,
                request.getFlightOfferId()
            );
            
            // Only record seatmap request for guest users if we actually got valid seat map data
            try {
                if (jwtService.isGuestToken(token)) {
                    // Check if we got valid seat map data (not just an error response)
                    if (seatMapData != null && seatMapData.has("data") && seatMapData.get("data").isArray() && seatMapData.get("data").size() > 0) {
                        guestAccessRepository.recordSeatmapRequest(clientIp);
                        logger.info("Recorded seatmap request for guest IP: {} after successful seat map retrieval", clientIp);
                    } else {
                        logger.info("Skipping seatmap request recording for guest IP: {} - no valid seat map data returned", clientIp);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to record seatmap request for IP {}: {}", clientIp, e.getMessage());
                // Don't fail the request if recording fails
            }
            
            return createSuccessResponse(response);
            
        } catch (SeatmapException e) {
            logger.error("Seatmap service error", e);
            return createErrorResponse(500, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing seat map request", e);
            return createErrorResponse(500, "Internal server error");
        }
    }
    
    private JsonNode getSeatMapBySource(SeatMapRequest request) throws SeatmapException {
        try {
            // Parse the flight offer data to determine the source
            JsonNode flightOfferData = objectMapper.readTree(request.getFlightOfferData());
            String dataSource = flightOfferData.path("dataSource").asText("AMADEUS");
            
            logger.info("Routing seat map request to {} based on flight data source", dataSource);
            
            if ("SABRE".equals(dataSource)) {
                // Extract flight details for Sabre API
                return getSeatMapFromSabre(flightOfferData);
            } else {
                // Default to Amadeus (handles both "AMADEUS" and missing source)
                return amadeusService.getSeatMapFromOfferData(request.getFlightOfferData());
            }
            
        } catch (Exception e) {
            logger.error("Error parsing flight offer data for source routing", e);
            // Fall back to Amadeus if parsing fails
            return amadeusService.getSeatMapFromOfferData(request.getFlightOfferData());
        }
    }
    
    private JsonNode getSeatMapFromSabre(JsonNode flightOfferData) throws SeatmapException {
        // Extract flight details from the flight offer data
        String carrierCode = "";
        String flightNumber = "";
        String departureDate = "";
        String origin = "";
        String destination = "";
        
        try {
            if (flightOfferData.has("itineraries") && flightOfferData.get("itineraries").isArray() && flightOfferData.get("itineraries").size() > 0) {
                JsonNode firstItinerary = flightOfferData.get("itineraries").get(0);
                if (firstItinerary.has("segments") && firstItinerary.get("segments").isArray() && firstItinerary.get("segments").size() > 0) {
                    JsonNode firstSegment = firstItinerary.get("segments").get(0);
                    
                    carrierCode = firstSegment.path("carrierCode").asText("");
                    flightNumber = firstSegment.path("number").asText("");
                    origin = firstSegment.path("departure").path("iataCode").asText("");
                    destination = firstSegment.path("arrival").path("iataCode").asText("");
                    
                    String departureDateTime = firstSegment.path("departure").path("at").asText("");
                    if (departureDateTime.length() >= 10) {
                        departureDate = departureDateTime.substring(0, 10); // Extract date part
                    }
                }
            }
            
            if (carrierCode.isEmpty() || flightNumber.isEmpty() || origin.isEmpty() || destination.isEmpty()) {
                throw new SeatmapException("Missing required flight details for Sabre seat map request");
            }
            
            logger.info("Calling Sabre seat map API for flight {}{}  from {} to {} on {}", carrierCode, flightNumber, origin, destination, departureDate);
            
            return sabreService.getSeatMapFromFlight(carrierCode, flightNumber, departureDate, origin, destination);
            
        } catch (Exception e) {
            logger.error("Error extracting flight details for Sabre seat map request", e);
            throw new SeatmapException("Failed to process Sabre seat map request", e);
        }
    }
    
    private APIGatewayProxyResponseEvent createSuccessResponse(SeatMapResponse response) {
        try {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(createCorsHeaders())
                .withBody(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.error("Error creating success response", e);
            return createErrorResponse(500, "Error formatting response");
        }
    }
    
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        try {
            SeatMapResponse response = SeatMapResponse.error(message);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(createCorsHeaders())
                .withBody(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.error("Error creating error response", e);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(createCorsHeaders())
                .withBody("{\"success\":false,\"message\":\"" + message + "\"}");
        }
    }
    
    /**
     * Extract client IP address from API Gateway request
     * Handles X-Forwarded-For header and fallback to source IP
     */
    private String extractClientIp(APIGatewayProxyRequestEvent event) {
        logger.info("Attempting to extract client IP from request");
        
        // First try X-Forwarded-For header (API Gateway adds this)
        Map<String, String> headers = event.getHeaders();
        logger.info("Request headers available: {}", headers != null);
        if (headers != null) {
            logger.info("Available headers: {}", headers.keySet());
            String forwardedFor = headers.get("X-Forwarded-For");
            logger.info("X-Forwarded-For header: '{}'", forwardedFor);
            if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
                // X-Forwarded-For can contain multiple IPs, first one is the original client
                String[] ips = forwardedFor.split(",");
                String clientIp = ips[0].trim();
                logger.info("Client IP extracted from X-Forwarded-For: {}", clientIp);
                return clientIp;
            }
            
            // Try other forwarded headers as fallback
            String xRealIp = headers.get("X-Real-IP");
            logger.info("X-Real-IP header: '{}'", xRealIp);
            if (xRealIp != null && !xRealIp.trim().isEmpty()) {
                logger.info("Client IP extracted from X-Real-IP: {}", xRealIp);
                return xRealIp.trim();
            }
        }
        
        // Fallback to source IP from request context
        logger.info("Request context available: {}", event.getRequestContext() != null);
        if (event.getRequestContext() != null) {
            logger.info("Request context identity available: {}", event.getRequestContext().getIdentity() != null);
            if (event.getRequestContext().getIdentity() != null) {
                String sourceIp = event.getRequestContext().getIdentity().getSourceIp();
                logger.info("Source IP from request context: '{}'", sourceIp);
                if (sourceIp != null && !sourceIp.trim().isEmpty()) {
                    logger.info("Client IP extracted from request context source IP: {}", sourceIp);
                    return sourceIp.trim();
                }
            }
        }
        
        // Last resort fallback
        logger.warn("Could not extract client IP from any source, using fallback 'unknown'");
        return "unknown";
    }
    
    private Map<String, String> createCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}