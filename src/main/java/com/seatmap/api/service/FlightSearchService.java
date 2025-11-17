package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.api.model.FlightSearchResponse;
import com.seatmap.common.exception.SeatmapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class FlightSearchService {
    private static final Logger logger = LoggerFactory.getLogger(FlightSearchService.class);
    
    private final AmadeusService amadeusService;
    private final SabreService sabreService;
    
    public FlightSearchService(AmadeusService amadeusService, SabreService sabreService) {
        this.amadeusService = amadeusService;
        this.sabreService = sabreService;
    }
    
    public FlightSearchResponse searchFlightsWithSeatmaps(FlightSearchRequest request) throws SeatmapException {
        return searchFlightsWithSeatmaps(
            request.getOrigin(),
            request.getDestination(), 
            request.getDepartureDate(),
            request.getTravelClass(),
            request.getFlightNumber(),
            request.getMaxResults()
        );
    }
    
    public FlightSearchResponse searchFlightsWithSeatmaps(String origin, String destination, String departureDate, String travelClass, String flightNumber, Integer maxResults) throws SeatmapException {
        logger.info("Searching flights with seatmaps from Amadeus and Sabre sources");
        
        // Search both sources concurrently using batch seat map requests
        CompletableFuture<List<FlightSearchResult>> amadeusFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return amadeusService.searchFlightsWithBatchSeatmaps(origin, destination, departureDate, travelClass, flightNumber, maxResults);
            } catch (Exception e) {
                logger.error("Error calling Amadeus API for batch flight search with seatmaps", e);
                return new ArrayList<>();
            }
        });
        
        CompletableFuture<List<FlightSearchResult>> sabreFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return sabreService.searchFlightsWithSeatmaps(origin, destination, departureDate, travelClass, flightNumber, maxResults);
            } catch (Exception e) {
                logger.error("Error calling Sabre API for flight search with seatmaps", e);
                return new ArrayList<>();
            }
        });
        
        try {
            // Wait for both API calls to complete
            List<FlightSearchResult> amadeusResults = amadeusFuture.get();
            List<FlightSearchResult> sabreResults = sabreFuture.get();
            
            // Mesh the results with Amadeus priority
            return meshFlightSearchResults(amadeusResults, sabreResults, maxResults);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error executing concurrent API calls", e);
            throw new SeatmapException("EXTERNAL_API_ERROR", "Error searching flights from multiple sources", 500, e);
        }
    }
    
    private FlightSearchResponse meshFlightSearchResults(List<FlightSearchResult> amadeusResults, List<FlightSearchResult> sabreResults, Integer maxResults) {
        logger.info("Meshing flight search results from Amadeus and Sabre");
        
        List<FlightSearchResult> allResults = new ArrayList<>();
        Map<String, FlightSearchResult> flightMap = new HashMap<>();
        
        // Add Amadeus results first (higher priority)
        for (FlightSearchResult result : amadeusResults) {
            String flightKey = createFlightKey(result);
            flightMap.put(flightKey, result);
            allResults.add(result);
        }
        
        // Add Sabre results only if not already present from Amadeus
        for (FlightSearchResult result : sabreResults) {
            String flightKey = createFlightKey(result);
            if (!flightMap.containsKey(flightKey)) {
                flightMap.put(flightKey, result);
                allResults.add(result);
            }
        }
        
        // Limit results to maxResults
        int limit = maxResults != null ? maxResults : 10;
        List<FlightSearchResult> limitedResults = allResults.subList(0, Math.min(allResults.size(), limit));
        
        // Log if no flights with seatmaps were found
        if (limitedResults.isEmpty()) {
            logger.warn("No flights found with available seatmap data");
        }
        
        // Build metadata
        FlightSearchResponse.SearchMetadata meta = new FlightSearchResponse.SearchMetadata(
            limitedResults.size(), 
            "AMADEUS,SABRE"
        );
        
        logger.info("Combined {} flights with seatmaps from Amadeus and Sabre sources", limitedResults.size());
        return new FlightSearchResponse(limitedResults, meta);
    }
    
    private String createFlightKey(FlightSearchResult result) {
        // Create unique key based on carrier, flight number, and route
        StringBuilder key = new StringBuilder();
        
        if (result.getItineraries() != null && !result.getItineraries().isEmpty()) {
            JsonNode firstItinerary = result.getItineraries().get(0);
            if (firstItinerary.has("segments") && firstItinerary.get("segments").isArray() && firstItinerary.get("segments").size() > 0) {
                JsonNode firstSegment = firstItinerary.get("segments").get(0);
                
                key.append(firstSegment.path("carrierCode").asText(""));
                key.append(firstSegment.path("number").asText(""));
                key.append(firstSegment.path("departure").path("iataCode").asText(""));
                key.append(firstSegment.path("arrival").path("iataCode").asText(""));
                String departureAt = firstSegment.path("departure").path("at").asText("");
                if (departureAt.length() >= 10) {
                    key.append(departureAt.substring(0, 10)); // Date only
                }
            }
        }
        
        return key.toString();
    }
}