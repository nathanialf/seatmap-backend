package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.model.FlightOffersRequest;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.api.service.SabreService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.common.exception.SeatmapException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FlightOffersHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(FlightOffersHandler.class);
    
    private final ObjectMapper objectMapper;
    private final AmadeusService amadeusService;
    private final SabreService sabreService;
    private final JwtService jwtService;
    private final Validator validator;
    
    public FlightOffersHandler() {
        this.objectMapper = new ObjectMapper();
        this.amadeusService = new AmadeusService();
        this.sabreService = new SabreService();
        this.jwtService = new JwtService();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        logger.info("Processing flight offers request");
        
        try {
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
            FlightOffersRequest request;
            try {
                request = objectMapper.readValue(event.getBody(), FlightOffersRequest.class);
            } catch (Exception e) {
                logger.error("Error parsing request body", e);
                return createErrorResponse(400, "Invalid request format");
            }
            
            // Validate request
            Set<ConstraintViolation<FlightOffersRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder errors = new StringBuilder();
                for (ConstraintViolation<FlightOffersRequest> violation : violations) {
                    errors.append(violation.getMessage()).append("; ");
                }
                return createErrorResponse(400, "Validation errors: " + errors.toString());
            }
            
            // Search for flight offers from both sources concurrently
            JsonNode combinedFlightOffers = searchFlightOffersFromAllSources(
                request.getOrigin(),
                request.getDestination(), 
                request.getDepartureDate(),
                request.getFlightNumber(),
                request.getMaxResults()
            );
            
            return createSuccessResponse(combinedFlightOffers);
            
        } catch (Exception e) {
            logger.error("Error processing flight offers request", e);
            return createErrorResponse(500, "Internal server error");
        }
    }
    
    private JsonNode searchFlightOffersFromAllSources(String origin, String destination, String departureDate, String flightNumber, Integer maxResults) throws SeatmapException {
        logger.info("Searching flight offers from Amadeus and Sabre sources");
        
        // Search both sources concurrently
        CompletableFuture<JsonNode> amadeusFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return amadeusService.searchFlightOffers(origin, destination, departureDate, flightNumber, maxResults);
            } catch (Exception e) {
                logger.error("Error calling Amadeus API", e);
                return objectMapper.createObjectNode().set("data", objectMapper.createArrayNode());
            }
        });
        
        CompletableFuture<JsonNode> sabreFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return sabreService.searchFlightSchedules(origin, destination, departureDate, flightNumber, maxResults);
            } catch (Exception e) {
                logger.error("Error calling Sabre API", e);
                return objectMapper.createObjectNode().set("data", objectMapper.createArrayNode());
            }
        });
        
        try {
            // Wait for both API calls to complete
            JsonNode amadeusResult = amadeusFuture.get();
            JsonNode sabreResult = sabreFuture.get();
            
            // Mesh the results with Amadeus priority
            return meshFlightOffers(amadeusResult, sabreResult, maxResults);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing concurrent API calls", e);
            throw new SeatmapException("EXTERNAL_API_ERROR", "Error searching flight offers from multiple sources", 500, e);
        }
    }
    
    private JsonNode meshFlightOffers(JsonNode amadeusResult, JsonNode sabreResult, Integer maxResults) {
        logger.info("Meshing flight offers from Amadeus and Sabre");
        
        ObjectNode combinedResult = objectMapper.createObjectNode();
        ArrayNode combinedData = objectMapper.createArrayNode();
        
        // Extract flight data arrays
        ArrayNode amadeusFlights = (ArrayNode) amadeusResult.get("data");
        ArrayNode sabreFlights = (ArrayNode) sabreResult.get("data");
        
        List<JsonNode> allFlights = new ArrayList<>();
        Map<String, JsonNode> flightMap = new HashMap<>();
        
        // Add Amadeus flights first (higher priority)
        if (amadeusFlights != null) {
            for (JsonNode flight : amadeusFlights) {
                String flightKey = createFlightKey(flight);
                flightMap.put(flightKey, addSourceTag(flight, "AMADEUS"));
                allFlights.add(flightMap.get(flightKey));
            }
        }
        
        // Add Sabre flights only if not already present from Amadeus
        if (sabreFlights != null) {
            for (JsonNode flight : sabreFlights) {
                String flightKey = createFlightKey(flight);
                if (!flightMap.containsKey(flightKey)) {
                    JsonNode sabreFlightWithSource = addSourceTag(flight, "SABRE");
                    flightMap.put(flightKey, sabreFlightWithSource);
                    allFlights.add(sabreFlightWithSource);
                }
            }
        }
        
        // Limit results to maxResults
        int limit = maxResults != null ? maxResults : 10;
        int count = 0;
        for (JsonNode flight : allFlights) {
            if (count >= limit) break;
            combinedData.add(flight);
            count++;
        }
        
        // Build response in Amadeus format
        combinedResult.set("data", combinedData);
        
        // Add metadata
        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("count", combinedData.size());
        meta.put("sources", "AMADEUS,SABRE");
        combinedResult.set("meta", meta);
        
        // Copy dictionaries from Amadeus if available
        if (amadeusResult.has("dictionaries")) {
            combinedResult.set("dictionaries", amadeusResult.get("dictionaries"));
        }
        
        logger.info("Combined {} flights from Amadeus and Sabre sources", combinedData.size());
        return combinedResult;
    }
    
    private String createFlightKey(JsonNode flight) {
        // Create unique key based on carrier, flight number, and route
        StringBuilder key = new StringBuilder();
        
        if (flight.has("itineraries") && flight.get("itineraries").isArray() && flight.get("itineraries").size() > 0) {
            JsonNode firstItinerary = flight.get("itineraries").get(0);
            if (firstItinerary.has("segments") && firstItinerary.get("segments").isArray() && firstItinerary.get("segments").size() > 0) {
                JsonNode firstSegment = firstItinerary.get("segments").get(0);
                
                key.append(firstSegment.path("carrierCode").asText(""));
                key.append(firstSegment.path("number").asText(""));
                key.append(firstSegment.path("departure").path("iataCode").asText(""));
                key.append(firstSegment.path("arrival").path("iataCode").asText(""));
                key.append(firstSegment.path("departure").path("at").asText("").substring(0, 10)); // Date only
            }
        }
        
        return key.toString();
    }
    
    private JsonNode addSourceTag(JsonNode flight, String source) {
        if (flight.isObject()) {
            ObjectNode flightObj = (ObjectNode) flight;
            flightObj.put("dataSource", source);
            
            // Ensure the flight has a unique ID that includes the source
            String originalId = flightObj.path("id").asText("");
            flightObj.put("id", source.toLowerCase() + "_" + originalId);
        }
        return flight;
    }
    
    private APIGatewayProxyResponseEvent createSuccessResponse(JsonNode data) {
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