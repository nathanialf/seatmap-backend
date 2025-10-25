package com.seatmap.auth.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.model.LoginRequest;
import com.seatmap.auth.model.RegisterRequest;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.repository.SessionRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.auth.service.AuthService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.auth.service.PasswordService;
import com.seatmap.email.service.EmailService;
import com.seatmap.common.exception.SeatmapException;
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

public class AuthHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AuthService authService;
    
    public AuthHandler() {
        this.objectMapper = new ObjectMapper();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
        
        // Initialize AWS services with explicit HTTP client to avoid conflicts
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
        
        UserRepository userRepository = new UserRepository(dynamoDbClient, usersTable);
        SessionRepository sessionRepository = new SessionRepository(dynamoDbClient, sessionsTable);
        GuestAccessRepository guestAccessRepository = new GuestAccessRepository(dynamoDbClient);
        PasswordService passwordService = new PasswordService();
        JwtService jwtService = new JwtService();
        EmailService emailService = new EmailService();
        
        this.authService = new AuthService(userRepository, sessionRepository, passwordService, jwtService, guestAccessRepository, emailService);
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        logger.info("Processing authentication request: {}", event.getPath());
        
        try {
            String path = event.getPath();
            String httpMethod = event.getHttpMethod();
            
            // Route based on path and method
            if ("POST".equals(httpMethod)) {
                switch (path) {
                    case "/auth/guest":
                        return handleGuestSession(event);
                    case "/auth/login":
                        return handleLogin(event);
                    case "/auth/register":
                        return handleRegister(event);
                    case "/auth/refresh":
                        return handleRefreshToken(event);
                    case "/auth/verify":
                        return handleVerifyEmail(event);
                    case "/auth/resend-verification":
                        return handleResendVerification(event);
                    default:
                        return createErrorResponse(404, "Endpoint not found");
                }
            } else if ("DELETE".equals(httpMethod) && "/auth/logout".equals(path)) {
                return handleLogout(event);
            }
            
            return createErrorResponse(405, "Method not allowed");
            
        } catch (SeatmapException e) {
            logger.error("Authentication service error", e);
            return createErrorResponse(e.getHttpStatus(), e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing authentication request", e);
            return createErrorResponse(500, "Internal server error");
        }
    }
    
    private APIGatewayProxyResponseEvent handleGuestSession(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Creating guest session");
        
        // Extract client IP from request
        String clientIp = extractClientIp(event);
        logger.debug("Client IP extracted: {}", clientIp);
        
        AuthResponse response = authService.createGuestSession(clientIp);
        return createSuccessResponse(response);
    }
    
    private APIGatewayProxyResponseEvent handleLogin(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Processing login request");
        
        // Parse request body
        LoginRequest request;
        try {
            request = objectMapper.readValue(event.getBody(), LoginRequest.class);
        } catch (Exception e) {
            logger.error("Error parsing login request body", e);
            return createErrorResponse(400, "Invalid request format");
        }
        
        // Validate request
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ConstraintViolation<LoginRequest> violation : violations) {
                errors.append(violation.getMessage()).append("; ");
            }
            return createErrorResponse(400, "Validation errors: " + errors.toString());
        }
        
        AuthResponse response = authService.login(request);
        return createSuccessResponse(response);
    }
    
    private APIGatewayProxyResponseEvent handleRegister(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Processing registration request");
        
        // Parse request body
        RegisterRequest request;
        try {
            request = objectMapper.readValue(event.getBody(), RegisterRequest.class);
        } catch (Exception e) {
            logger.error("Error parsing registration request body", e);
            return createErrorResponse(400, "Invalid request format");
        }
        
        // Validate request
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ConstraintViolation<RegisterRequest> violation : violations) {
                errors.append(violation.getMessage()).append("; ");
            }
            return createErrorResponse(400, "Validation errors: " + errors.toString());
        }
        
        AuthResponse response = authService.register(request);
        return createSuccessResponse(response);
    }
    
    private APIGatewayProxyResponseEvent handleRefreshToken(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Processing token refresh request");
        
        // Get token from Authorization header
        String authHeader = event.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return createErrorResponse(401, "Authorization token required");
        }
        
        String token = authHeader.substring(7);
        AuthResponse response = authService.refreshToken(token);
        return createSuccessResponse(response);
    }
    
    private APIGatewayProxyResponseEvent handleLogout(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Processing logout request");
        
        // Get token from Authorization header
        String authHeader = event.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return createErrorResponse(401, "Authorization token required");
        }
        
        String token = authHeader.substring(7);
        authService.logout(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logged out successfully");
        
        return createSuccessResponse(response);
    }
    
    private APIGatewayProxyResponseEvent createSuccessResponse(Object response) {
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", message);
            
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(createCorsHeaders())
                .withBody(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            logger.error("Error creating error response", e);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(createCorsHeaders())
                .withBody("{\"success\":false,\"message\":\"" + message + "\"}");
        }
    }
    
    private APIGatewayProxyResponseEvent handleVerifyEmail(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Processing email verification request");
        
        // Get verification token from query parameter
        Map<String, String> queryParams = event.getQueryStringParameters();
        if (queryParams == null || !queryParams.containsKey("token")) {
            return createErrorResponse(400, "Verification token required");
        }
        
        String verificationToken = queryParams.get("token");
        AuthResponse response = authService.verifyEmail(verificationToken);
        return createSuccessResponse(response);
    }
    
    private APIGatewayProxyResponseEvent handleResendVerification(APIGatewayProxyRequestEvent event) throws SeatmapException {
        logger.info("Processing resend verification request");
        
        // Parse request body for email
        Map<String, String> requestBody;
        try {
            requestBody = objectMapper.readValue(event.getBody(), Map.class);
        } catch (Exception e) {
            logger.error("Error parsing resend verification request body", e);
            return createErrorResponse(400, "Invalid request format");
        }
        
        String email = requestBody.get("email");
        if (email == null || email.trim().isEmpty()) {
            return createErrorResponse(400, "Email is required");
        }
        
        AuthResponse response = authService.resendVerificationEmail(email);
        return createSuccessResponse(response);
    }
    
    /**
     * Extract client IP address from API Gateway request
     * Handles X-Forwarded-For header and fallback to source IP
     */
    private String extractClientIp(APIGatewayProxyRequestEvent event) {
        // First try X-Forwarded-For header (API Gateway adds this)
        Map<String, String> headers = event.getHeaders();
        if (headers != null) {
            String forwardedFor = headers.get("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
                // X-Forwarded-For can contain multiple IPs, first one is the original client
                String[] ips = forwardedFor.split(",");
                String clientIp = ips[0].trim();
                logger.debug("Client IP from X-Forwarded-For: {}", clientIp);
                return clientIp;
            }
            
            // Try other forwarded headers as fallback
            String xRealIp = headers.get("X-Real-IP");
            if (xRealIp != null && !xRealIp.trim().isEmpty()) {
                logger.debug("Client IP from X-Real-IP: {}", xRealIp);
                return xRealIp.trim();
            }
        }
        
        // Fallback to source IP from request context
        if (event.getRequestContext() != null && 
            event.getRequestContext().getIdentity() != null) {
            String sourceIp = event.getRequestContext().getIdentity().getSourceIp();
            if (sourceIp != null && !sourceIp.trim().isEmpty()) {
                logger.debug("Client IP from source IP: {}", sourceIp);
                return sourceIp.trim();
            }
        }
        
        // Last resort fallback
        logger.warn("Could not extract client IP, using fallback");
        return "unknown";
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