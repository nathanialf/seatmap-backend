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
        boolean includeRaw = Boolean.TRUE.equals(request.getIncludeRawFlightOffer());
        return searchFlightsWithSeatmaps(
            request.getOrigin(),
            request.getDestination(), 
            request.getDepartureDate(),
            request.getTravelClass(),
            request.getAirlineCode(),
            request.getFlightNumber(),
            request.getMaxResults(),
            request.getOffset(),
            includeRaw
        );
    }
    
    public FlightSearchResponse searchFlightsWithSeatmaps(String origin, String destination, String departureDate, String travelClass, String airlineCode, String flightNumber, Integer maxResults) throws SeatmapException {
        return searchFlightsWithSeatmaps(origin, destination, departureDate, travelClass, airlineCode, flightNumber, maxResults, 0, false);
    }
    
    public FlightSearchResponse searchFlightsWithSeatmaps(String origin, String destination, String departureDate, String travelClass, String airlineCode, String flightNumber, Integer maxResults, boolean includeRawFlightOffer) throws SeatmapException {
        return searchFlightsWithSeatmaps(origin, destination, departureDate, travelClass, airlineCode, flightNumber, maxResults, 0, includeRawFlightOffer);
    }
    
    public FlightSearchResponse searchFlightsWithSeatmaps(String origin, String destination, String departureDate, String travelClass, String airlineCode, String flightNumber, Integer maxResults, Integer offset, boolean includeRawFlightOffer) throws SeatmapException {
        logger.info("Searching flights with seatmaps from Amadeus and Sabre sources");
        
        // Normalize offset (ensure null is converted to 0)
        Integer normalizedOffset = offset != null ? offset : 0;
        
        // Search Amadeus only (Sabre temporarily disabled)
        CompletableFuture<List<FlightSearchResult>> amadeusFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<FlightSearchResult> results = amadeusService.searchFlightsWithBatchSeatmaps(origin, destination, departureDate, travelClass, airlineCode, flightNumber, maxResults, normalizedOffset);
                return processResultsForRawData(results, includeRawFlightOffer);
            } catch (Exception e) {
                logger.error("Error calling Amadeus API for batch flight search with seatmaps", e);
                return new ArrayList<>();
            }
        });
        
        try {
            // Wait for Amadeus API call to complete
            List<FlightSearchResult> amadeusResults = amadeusFuture.get();
            
            // Return only Amadeus results (Sabre temporarily disabled)
            return createFlightSearchResponse(amadeusResults, maxResults, normalizedOffset);
            
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
    
    private List<FlightSearchResult> processResultsForRawData(List<FlightSearchResult> results, boolean includeRawFlightOffer) {
        if (!includeRawFlightOffer) {
            return results; // No modification needed if raw data not requested
        }
        
        // Convert each result to include raw flight offer data
        List<FlightSearchResult> processedResults = new ArrayList<>();
        for (FlightSearchResult result : results) {
            try {
                // Convert back to JsonNode to reconstruct with raw data
                JsonNode resultAsJson = result.toJsonNode();
                
                // Create new FlightSearchResult with raw data included
                FlightSearchResult newResult = new FlightSearchResult(
                    resultAsJson, 
                    result.getSeatMap(), 
                    result.isSeatMapAvailable(), 
                    result.getSeatMapError(), 
                    true
                );
                processedResults.add(newResult);
            } catch (Exception e) {
                logger.warn("Error processing result for raw data inclusion: {}", e.getMessage());
                // Fall back to original result without raw data
                processedResults.add(result);
            }
        }
        
        return processedResults;
    }
    
    private FlightSearchResponse createFlightSearchResponse(List<FlightSearchResult> results, Integer maxResults) {
        return createFlightSearchResponse(results, maxResults, 0);
    }
    
    private FlightSearchResponse createFlightSearchResponse(List<FlightSearchResult> results, Integer maxResults, Integer offset) {
        // Don't limit results here - they're already limited by the API call
        int limit = maxResults != null ? maxResults : 10;
        int pageOffset = offset != null ? offset : 0;
        
        // Log if no flights with seatmaps were found
        if (results.isEmpty()) {
            logger.warn("No flights found with available seatmap data");
        }
        
        // Create pagination metadata
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(
            pageOffset,
            limit,
            -1, // Total unknown from Amadeus API
            results.size() >= limit, // hasNext: true if we got a full page
            pageOffset > 0 // hasPrevious: true if offset > 0
        );
        
        // Build metadata (only Amadeus for now)
        FlightSearchResponse.SearchMetadata meta = new FlightSearchResponse.SearchMetadata(
            results.size(), 
            "AMADEUS",
            null,
            pagination
        );
        
        logger.info("Found {} flights with seatmaps from Amadeus (offset: {}, limit: {})", results.size(), pageOffset, limit);
        return new FlightSearchResponse(results, meta);
    }
}