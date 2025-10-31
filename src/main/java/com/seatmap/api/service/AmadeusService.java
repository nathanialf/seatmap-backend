package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapApiException;
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
    
    public JsonNode getSeatMap(String flightNumber, String departureDate, String origin, String destination) throws SeatmapApiException {
        try {
            ensureValidToken();
            
            // Step 1: Search for flight offers using Flight Offers Search API (any travel class)
            JsonNode flightOffers = searchFlightOffersInternal(origin, destination, departureDate, null, flightNumber, 10);
            
            if (flightOffers == null || !flightOffers.has("data") || flightOffers.get("data").size() == 0) {
                throw new SeatmapApiException("No flight offers found for the specified criteria");
            }
            
            // Step 2: Get seat map using the first flight offer
            JsonNode firstOffer = flightOffers.get("data").get(0);
            return getSeatMapFromOffer(firstOffer);
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    public JsonNode searchFlightOffers(String origin, String destination, String departureDate, String travelClass, String flightNumber, Integer maxResults) throws SeatmapApiException {
        try {
            ensureValidToken();
            return searchFlightOffersInternal(origin, destination, departureDate, travelClass, flightNumber, maxResults);
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    public JsonNode getSeatMapFromOfferData(String flightOfferData) throws SeatmapApiException {
        try {
            ensureValidToken();
            
            // Parse the flight offer data
            JsonNode flightOffer = objectMapper.readTree(flightOfferData);
            return getSeatMapFromOffer(flightOffer);
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    private JsonNode searchFlightOffersInternal(String origin, String destination, String departureDate, String travelClass, String flightNumber, Integer maxResults) throws SeatmapApiException, IOException, InterruptedException {
        int max = maxResults != null ? maxResults : 10;
        
        // Build base URL - only include travelClass if specified (minimum cabin quality)
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(String.format("https://%s/v2/shopping/flight-offers?originLocationCode=%s&destinationLocationCode=%s&departureDate=%s&adults=1&max=%d",
            endpoint,
            URLEncoder.encode(origin, StandardCharsets.UTF_8),
            URLEncoder.encode(destination, StandardCharsets.UTF_8),
            URLEncoder.encode(departureDate, StandardCharsets.UTF_8),
            max
        ));
        
        // Add travelClass parameter only if specified (searches minimum quality or higher)
        if (travelClass != null && !travelClass.trim().isEmpty()) {
            urlBuilder.append("&travelClass=").append(URLEncoder.encode(travelClass, StandardCharsets.UTF_8));
        }
        
        String url = urlBuilder.toString();
        
        // Add flight number filter if specified
        if (flightNumber != null && !flightNumber.trim().isEmpty()) {
            // Extract carrier code (first 2-3 characters) and flight number
            String carrierCode = flightNumber.replaceAll("\\d", "").trim();
            if (!carrierCode.isEmpty()) {
                url += "&includedAirlineCodes=" + URLEncoder.encode(carrierCode, StandardCharsets.UTF_8);
            }
        }
        
        logger.info("Searching flight offers: {}", url);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode result = objectMapper.readTree(response.body());
            logger.info("Found {} flight offers", result.has("data") ? result.get("data").size() : 0);
            return result;
        } else {
            logger.error("Flight offers search error: {} - {}", response.statusCode(), response.body());
            throw new SeatmapApiException("Failed to search flight offers: " + response.statusCode());
        }
    }
    
    private JsonNode getSeatMapFromOffer(JsonNode flightOffer) throws SeatmapApiException, IOException, InterruptedException {
        String url = "https://" + endpoint + "/v1/shopping/seatmaps";
        
        // Create request body with flight offer
        String requestBody = objectMapper.writeValueAsString(
            objectMapper.createObjectNode()
                .set("data", objectMapper.createArrayNode().add(flightOffer))
        );
        
        logger.info("Getting seat map for flight offer: {}", flightOffer.get("id"));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode result = objectMapper.readTree(response.body());
            logger.info("Successfully retrieved seat map data");
            return result;
        } else {
            logger.error("Seat map API error: {} - {}", response.statusCode(), response.body());
            throw new SeatmapApiException("Failed to retrieve seat map: " + response.statusCode());
        }
    }
    
    private void ensureValidToken() throws SeatmapApiException {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
            refreshAccessToken();
        }
    }
    
    private void refreshAccessToken() throws SeatmapApiException {
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
                throw new SeatmapApiException("Failed to authenticate with Amadeus API");
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error refreshing Amadeus access token", e);
            throw new SeatmapApiException("Network error during authentication", e);
        }
    }
}