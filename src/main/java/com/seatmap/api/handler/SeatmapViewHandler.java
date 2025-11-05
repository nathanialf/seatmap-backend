package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.model.SeatmapViewRequest;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.repository.SessionRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.auth.service.PasswordService;
import com.seatmap.auth.service.UserUsageLimitsService;
import com.seatmap.common.model.User;
import com.seatmap.email.service.EmailService;
import io.jsonwebtoken.Claims;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SeatmapViewHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SeatmapViewHandler.class);
    
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final JwtService jwtService;
    private final AuthService authService;
    private final UserUsageLimitsService userUsageLimitsService;
    private final GuestAccessRepository guestAccessRepository;
    
    public SeatmapViewHandler() {
        this.objectMapper = new ObjectMapper();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        this.jwtService = new JwtService();
        
        // Initialize repositories with explicit HTTP client to avoid conflicts
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
        
        // Get environment for table names
        String environment = System.getenv("ENVIRONMENT");
        if (environment == null) environment = "dev";
        
        // Initialize repositories with table names
        String usersTable = "seatmap-users-" + environment;
        String sessionsTable = "seatmap-sessions-" + environment;
        String guestAccessTable = "seatmap-guest-access-" + environment;
        
        this.guestAccessRepository = new GuestAccessRepository(dynamoDbClient);
        
        // Initialize AuthService with all dependencies for token validation
        UserRepository userRepository = new UserRepository(dynamoDbClient, usersTable);
        SessionRepository sessionRepository = new SessionRepository(dynamoDbClient, sessionsTable);
        PasswordService passwordService = new PasswordService();
        EmailService emailService = new EmailService();
        
        this.authService = new AuthService(userRepository, sessionRepository, passwordService, jwtService, guestAccessRepository, emailService);
        this.userUsageLimitsService = new UserUsageLimitsService(dynamoDbClient, environment);
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        logger.info("Processing seatmap view tracking request");
        
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
            
            // Parse view request
            SeatmapViewRequest request;
            try {
                request = objectMapper.readValue(event.getBody(), SeatmapViewRequest.class);
            } catch (Exception e) {
                logger.error("Error parsing request body", e);
                return createErrorResponse(400, "Invalid request format");
            }
            
            // Validate request
            Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                StringBuilder errors = new StringBuilder();
                for (ConstraintViolation<SeatmapViewRequest> violation : violations) {
                    errors.append(violation.getMessage()).append("; ");
                }
                return createErrorResponse(400, "Validation errors: " + errors.toString());
            }
            
            // Check limits and record usage based on user type
            APIGatewayProxyResponseEvent limitCheckResponse = checkLimitsAndRecordUsage(token, request, event);
            if (limitCheckResponse != null) {
                return limitCheckResponse; // Return error response if limits exceeded
            }
            
            // Return simple success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Seatmap view recorded");
            
            return createSuccessResponse(response);
            
        } catch (Exception e) {
            logger.error("Error processing seatmap view request", e);
            return createErrorResponse(500, "Internal server error");
        }
    }
    
    /**
     * Check usage limits and record seatmap view if allowed
     * Returns null if successful, or error response if limits exceeded
     */
    private APIGatewayProxyResponseEvent checkLimitsAndRecordUsage(String token, SeatmapViewRequest request, APIGatewayProxyRequestEvent event) {
        try {
            if (jwtService.isGuestToken(token)) {
                // Check and record for guest users
                String clientIp = extractClientIp(event);
                
                // Check if guest can make another seatmap request
                if (!guestAccessRepository.canMakeSeatmapRequest(clientIp)) {
                    String denialMessage = guestAccessRepository.getSeatmapDenialMessage(clientIp);
                    logger.warn("Seatmap view denied for guest IP {}: {}", clientIp, denialMessage);
                    return createErrorResponse(403, denialMessage);
                }
                
                // Record the usage
                guestAccessRepository.recordSeatmapRequest(clientIp);
                logger.info("Recorded seatmap view for guest IP: {} for flight: {}", clientIp, request.getFlightId());
                
            } else {
                // Check and record for authenticated users
                User user = authService.validateToken(token);
                
                // Check if user can make another seatmap request
                if (!userUsageLimitsService.canMakeSeatmapRequest(user)) {
                    String denialMessage = userUsageLimitsService.getSeatmapLimitMessage(user);
                    logger.warn("Seatmap view denied for user {}: {}", user.getUserId(), denialMessage);
                    return createErrorResponse(403, denialMessage);
                }
                
                // Record the usage
                userUsageLimitsService.recordSeatmapRequest(user);
                logger.info("Recorded seatmap view for user: {} for flight: {}", user.getUserId(), request.getFlightId());
            }
            
            return null; // Success - no error response needed
            
        } catch (Exception e) {
            logger.error("Failed to check limits or record seatmap view for flight {}: {}", request.getFlightId(), e.getMessage());
            return createErrorResponse(500, "Error processing seatmap view request");
        }
    }
    
    /**
     * Extract client IP address from API Gateway request
     * Handles X-Forwarded-For header and fallback to source IP
     */
    private String extractClientIp(APIGatewayProxyRequestEvent event) {
        logger.debug("Attempting to extract client IP from request");
        
        // First try X-Forwarded-For header (API Gateway adds this)
        Map<String, String> headers = event.getHeaders();
        if (headers != null) {
            String forwardedFor = headers.get("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
                // X-Forwarded-For can contain multiple IPs, first one is the original client
                String[] ips = forwardedFor.split(",");
                String clientIp = ips[0].trim();
                logger.debug("Client IP extracted from X-Forwarded-For: {}", clientIp);
                return clientIp;
            }
            
            // Try other forwarded headers as fallback
            String xRealIp = headers.get("X-Real-IP");
            if (xRealIp != null && !xRealIp.trim().isEmpty()) {
                logger.debug("Client IP extracted from X-Real-IP: {}", xRealIp);
                return xRealIp.trim();
            }
        }
        
        // Fallback to source IP from request context
        if (event.getRequestContext() != null && event.getRequestContext().getIdentity() != null) {
            String sourceIp = event.getRequestContext().getIdentity().getSourceIp();
            if (sourceIp != null && !sourceIp.trim().isEmpty()) {
                logger.debug("Client IP extracted from request context source IP: {}", sourceIp);
                return sourceIp.trim();
            }
        }
        
        // Last resort fallback
        logger.warn("Could not extract client IP from any source, using fallback 'unknown'");
        return "unknown";
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
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.put("Content-Type", "application/json");
        return headers;
    }
}