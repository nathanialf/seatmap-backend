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
import com.seatmap.auth.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final JwtService jwtService;
    
    public SeatMapHandler() {
        this.objectMapper = new ObjectMapper();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.amadeusService = new AmadeusService();
        this.jwtService = new JwtService();
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
            
            // Check if this is a guest token and apply limits
            try {
                if (jwtService.isGuestToken(token)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> guestLimits = (Map<String, Object>) claims.get("guestLimits");
                    
                    if (guestLimits != null) {
                        int flightsViewed = (Integer) guestLimits.get("flightsViewed");
                        int maxFlights = (Integer) guestLimits.get("maxFlights");
                        
                        if (flightsViewed >= maxFlights) {
                            return createErrorResponse(403, "Guest seat map limit reached. Please register for unlimited access.");
                        }
                    }
                }
            } catch (com.seatmap.common.exception.SeatmapException e) {
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
            
            // Call Amadeus API
            JsonNode seatMapData = amadeusService.getSeatMap(
                request.getFlightNumber(),
                request.getDepartureDate(),
                request.getOrigin(),
                request.getDestination()
            );
            
            // Create successful response
            SeatMapResponse response = SeatMapResponse.success(
                seatMapData,
                request.getFlightNumber(),
                request.getDepartureDate(),
                request.getOrigin(),
                request.getDestination()
            );
            
            return createSuccessResponse(response);
            
        } catch (SeatmapException e) {
            logger.error("Seatmap service error", e);
            return createErrorResponse(500, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing seat map request", e);
            return createErrorResponse(500, "Internal server error");
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
    
    private Map<String, String> createCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}