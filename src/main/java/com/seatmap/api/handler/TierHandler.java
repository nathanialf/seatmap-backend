package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.TierDefinition;
import com.seatmap.common.repository.TierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TierHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(TierHandler.class);
    
    private final ObjectMapper objectMapper;
    private final TierRepository tierRepository;
    
    public TierHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Initialize AWS services
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
        
        // Get environment
        String environment = System.getenv("ENVIRONMENT");
        if (environment == null) environment = "dev";
        
        // Initialize repository with table name
        String tiersTable = "seatmap-tiers-" + environment;
        this.tierRepository = new TierRepository(dynamoDbClient, tiersTable);
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            String httpMethod = event.getHttpMethod();
            String path = event.getPath();
            
            logger.info("Processing {} request to {}", httpMethod, path);
            
            if ("GET".equals(httpMethod)) {
                if ("/tiers".equals(path)) {
                    return handleGetAllTiers(event);
                } else if (path.startsWith("/tiers/")) {
                    String tierName = path.substring("/tiers/".length());
                    return handleGetTierByName(tierName);
                }
            }
            
            return createErrorResponse(404, "Endpoint not found");
            
        } catch (SeatmapException e) {
            logger.error("Seatmap error in tier handler", e);
            return createErrorResponse(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in tier handler", e);
            return createErrorResponse(500, "Internal server error");
        }
    }
    
    private APIGatewayProxyResponseEvent handleGetAllTiers(APIGatewayProxyRequestEvent event) throws SeatmapException {
        // Check for region filter query parameter
        String region = null;
        if (event.getQueryStringParameters() != null) {
            region = event.getQueryStringParameters().get("region");
        }
        
        List<TierDefinition> tiers;
        if (region != null && !region.trim().isEmpty()) {
            logger.info("Fetching active publicly accessible tiers for region: {}", region);
            tiers = tierRepository.findByRegion(region.trim().toUpperCase());
            
            // Filter to only publicly accessible and active tiers
            tiers = tiers.stream()
                    .filter(tier -> tier.getPubliclyAccessible() && tier.getActive())
                    .collect(Collectors.toList());
        } else {
            logger.info("Fetching all active publicly accessible tiers");
            List<TierDefinition> allActiveTiers = tierRepository.findAllActive();
            
            // Filter to only publicly accessible tiers
            tiers = allActiveTiers.stream()
                    .filter(tier -> tier.getPubliclyAccessible())
                    .collect(Collectors.toList());
        }
        
        logger.info("Found {} publicly accessible tiers{}", tiers.size(), 
                   region != null ? " for region " + region : "");
        
        Map<String, Object> response = new HashMap<>();
        response.put("tiers", tiers);
        response.put("total", tiers.size());
        if (region != null && !region.trim().isEmpty()) {
            response.put("region", region.trim().toUpperCase());
        }
        
        return createSuccessResponse(response);
    }
    
    private APIGatewayProxyResponseEvent handleGetTierByName(String tierName) throws SeatmapException {
        logger.info("Fetching tier by name: {}", tierName);
        
        if (tierName == null || tierName.trim().isEmpty()) {
            return createErrorResponse(400, "Tier name is required");
        }
        
        Optional<TierDefinition> tierOpt = tierRepository.findByTierName(tierName.toUpperCase());
        
        if (tierOpt.isEmpty()) {
            logger.warn("Tier not found: {}", tierName);
            return createErrorResponse(404, "Tier not found");
        }
        
        TierDefinition tier = tierOpt.get();
        
        // Check if tier is publicly accessible and active
        if (!tier.getActive()) {
            logger.warn("Tier is not active: {}", tierName);
            return createErrorResponse(404, "Tier not found");
        }
        
        if (!tier.getPubliclyAccessible()) {
            logger.warn("Tier is not publicly accessible: {}", tierName);
            return createErrorResponse(404, "Tier not found");
        }
        
        logger.info("Successfully found tier: {}", tierName);
        return createSuccessResponse(tier);
    }
    
    private APIGatewayProxyResponseEvent createSuccessResponse(Object data) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(getCorsHeaders())
                .withBody(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.error("Error creating success response", e);
            return createErrorResponse(500, "Error creating response");
        }
    }
    
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", message);
            
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(getCorsHeaders())
                .withBody(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            logger.error("Error creating error response", e);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(getCorsHeaders())
                .withBody("{\"success\":false,\"message\":\"Internal server error\"}");
        }
    }
    
    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key");
        headers.put("Access-Control-Allow-Methods", "GET,OPTIONS");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}