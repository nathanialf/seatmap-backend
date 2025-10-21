package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class AmadeusService {
    private static final Logger logger = LoggerFactory.getLogger(AmadeusService.class);
    
    private final String apiKey;
    private final String apiSecret;
    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private String accessToken;
    private long tokenExpiresAt;
    
    public AmadeusService() {
        this.apiKey = System.getenv("AMADEUS_API_KEY");
        this.apiSecret = System.getenv("AMADEUS_API_SECRET");
        this.endpoint = System.getenv("AMADEUS_ENDPOINT");
        
        if (apiKey == null || apiSecret == null || endpoint == null) {
            throw new IllegalStateException("Amadeus API credentials not configured");
        }
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public JsonNode getSeatMap(String flightNumber, String departureDate, String origin, String destination) throws SeatmapException {
        try {
            ensureValidToken();
            
            String url = String.format("https://%s/v1/shopping/seatmaps?flightNumber=%s&departureDate=%s&origin=%s&destination=%s",
                endpoint,
                URLEncoder.encode(flightNumber, StandardCharsets.UTF_8),
                URLEncoder.encode(departureDate, StandardCharsets.UTF_8),
                URLEncoder.encode(origin, StandardCharsets.UTF_8),
                URLEncoder.encode(destination, StandardCharsets.UTF_8)
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            } else {
                logger.error("Amadeus API error: {} - {}", response.statusCode(), response.body());
                throw new SeatmapException("Failed to retrieve seat map: " + response.statusCode());
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API", e);
            throw new SeatmapException("Network error calling Amadeus API", e);
        }
    }
    
    private void ensureValidToken() throws SeatmapException {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
            refreshAccessToken();
        }
    }
    
    private void refreshAccessToken() throws SeatmapException {
        try {
            String credentials = Base64.getEncoder().encodeToString((apiKey + ":" + apiSecret).getBytes());
            String requestBody = "grant_type=client_credentials";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + endpoint + "/v1/security/oauth2/token"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode tokenResponse = objectMapper.readTree(response.body());
                this.accessToken = tokenResponse.get("access_token").asText();
                int expiresIn = tokenResponse.get("expires_in").asInt();
                this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L) - 60000; // Refresh 1 minute early
                
                logger.info("Successfully refreshed Amadeus access token");
            } else {
                logger.error("Failed to get Amadeus access token: {} - {}", response.statusCode(), response.body());
                throw new SeatmapException("Failed to authenticate with Amadeus API");
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error refreshing Amadeus access token", e);
            throw new SeatmapException("Network error during authentication", e);
        }
    }
}