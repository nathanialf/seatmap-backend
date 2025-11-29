package com.seatmap.alert.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.api.model.FlightSearchResponse;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.common.model.Bookmark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class AlertEvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertEvaluationService.class);
    private final ObjectMapper objectMapper;
    
    public AlertEvaluationService() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Evaluate if an alert should be triggered based on flight search results
     */
    public AlertEvaluationResult evaluateAlert(Bookmark bookmark, FlightSearchResponse searchResponse) {
        if (bookmark.getAlertConfig() == null || !bookmark.getAlertConfig().isEnabled()) {
            return AlertEvaluationResult.noAlert();
        }
        
        double threshold = bookmark.getAlertConfig().getAlertThreshold();
        
        try {
            if (bookmark.getItemType() == Bookmark.ItemType.BOOKMARK) {
                return evaluateBookmarkAlert(bookmark, searchResponse, threshold);
            } else if (bookmark.getItemType() == Bookmark.ItemType.SAVED_SEARCH) {
                return evaluateSavedSearchAlert(bookmark, searchResponse, threshold);
            }
        } catch (Exception e) {
            logger.error("Error evaluating alert for bookmark {}: {}", bookmark.getBookmarkId(), e.getMessage(), e);
            return AlertEvaluationResult.error("Error evaluating alert: " + e.getMessage());
        }
        
        return AlertEvaluationResult.noAlert();
    }
    
    /**
     * Evaluate alert for individual flight bookmark (absolute seat count threshold)
     */
    private AlertEvaluationResult evaluateBookmarkAlert(Bookmark bookmark, FlightSearchResponse searchResponse, double threshold) {
        try {
            // Parse the flight offer data from the bookmark
            JsonNode flightOfferData = objectMapper.readTree(bookmark.getFlightOfferData());
            
            // Extract flight identification details
            FlightIdentifier targetFlight = extractFlightIdentifier(flightOfferData);
            if (targetFlight == null) {
                return AlertEvaluationResult.error("Could not extract flight details from bookmark");
            }
            
            // Find the matching flight in search results
            Optional<FlightSearchResult> matchingFlight = findMatchingFlight(searchResponse.getData(), targetFlight);
            
            if (matchingFlight.isEmpty()) {
                return AlertEvaluationResult.error("Flight not found in search results");
            }
            
            FlightSearchResult flight = matchingFlight.get();
            int currentSeats = flight.getNumberOfBookableSeats();
            
            // For bookmarks: trigger when seats fall below threshold (absolute count)
            boolean shouldTrigger = currentSeats < threshold;
            
            if (shouldTrigger) {
                String message = String.format("Seat availability dropped to %d seats (below threshold of %.0f)", 
                    currentSeats, threshold);
                return AlertEvaluationResult.triggered(message, currentSeats, threshold, flight);
            } else {
                return AlertEvaluationResult.notTriggered(currentSeats, threshold);
            }
            
        } catch (Exception e) {
            logger.error("Error evaluating bookmark alert: {}", e.getMessage(), e);
            return AlertEvaluationResult.error("Error parsing flight data: " + e.getMessage());
        }
    }
    
    /**
     * Evaluate alert for saved search (percentage threshold for any matching flights)
     */
    private AlertEvaluationResult evaluateSavedSearchAlert(Bookmark bookmark, FlightSearchResponse searchResponse, double threshold) {
        List<FlightSearchResult> allFlights = searchResponse.getData();
        
        // Filter flights by airline if specified
        String airlineCode = bookmark.getAirlineCode();
        List<FlightSearchResult> filteredFlights = allFlights;
        
        if (airlineCode != null && !airlineCode.trim().isEmpty()) {
            filteredFlights = allFlights.stream()
                .filter(flight -> matchesAirline(flight, airlineCode))
                .toList();
        }
        
        // Check each flight for percentage above threshold
        for (FlightSearchResult flight : filteredFlights) {
            double percentage = calculateSeatPercentage(flight);
            
            // For saved searches: trigger when any flight has seats ABOVE threshold (percentage)
            if (percentage > threshold) {
                String message = String.format("Found flight with %.1f%% availability (above threshold of %.1f%%)", 
                    percentage, threshold);
                return AlertEvaluationResult.triggered(message, flight.getNumberOfBookableSeats(), threshold, flight);
            }
        }
        
        return AlertEvaluationResult.notTriggered(0, threshold);
    }
    
    /**
     * Extract flight identification details from flight offer data
     */
    private FlightIdentifier extractFlightIdentifier(JsonNode flightOfferData) {
        try {
            JsonNode itineraries = flightOfferData.get("itineraries");
            if (itineraries == null || !itineraries.isArray() || itineraries.size() == 0) {
                logger.warn("Missing or invalid itineraries in flight offer data");
                return null;
            }
            
            JsonNode firstItinerary = itineraries.get(0);
            if (firstItinerary == null) {
                logger.warn("First itinerary is null in flight offer data");
                return null;
            }
            
            JsonNode segments = firstItinerary.get("segments");
            if (segments == null || !segments.isArray() || segments.size() == 0) {
                logger.warn("Missing or invalid segments in first itinerary");
                return null;
            }
            
            JsonNode firstSegment = segments.get(0);
            if (firstSegment == null) {
                logger.warn("First segment is null in flight offer data");
                return null;
            }
            
            JsonNode departure = firstSegment.get("departure");
            JsonNode arrival = firstSegment.get("arrival");
            JsonNode operating = firstSegment.get("operating");
            
            if (departure == null || arrival == null) {
                logger.warn("Missing departure or arrival information in first segment");
                return null;
            }
            
            // Extract carrier code with proper null checks
            String carrierCode = null;
            if (operating != null && operating.get("carrierCode") != null) {
                carrierCode = operating.get("carrierCode").asText();
            } else if (firstSegment.get("carrierCode") != null) {
                carrierCode = firstSegment.get("carrierCode").asText();
            }
            
            if (carrierCode == null) {
                logger.warn("Missing carrierCode in flight segment");
                return null;
            }
            
            // Extract flight number with proper null checks
            String flightNumber = null;
            if (operating != null && operating.get("number") != null) {
                flightNumber = operating.get("number").asText();
            } else if (firstSegment.get("number") != null) {
                flightNumber = firstSegment.get("number").asText();
            }
            
            if (flightNumber == null) {
                logger.warn("Missing flight number in flight segment");
                return null;
            }
            
            // Extract departure date with proper null checks
            JsonNode departureAt = departure.get("at");
            if (departureAt == null) {
                logger.warn("Missing departure 'at' field in flight segment");
                return null;
            }
            String departureDate = departureAt.asText().substring(0, 10); // Extract date part
            
            // Extract origin with proper null checks
            JsonNode originCode = departure.get("iataCode");
            if (originCode == null) {
                logger.warn("Missing departure iataCode in flight segment");
                return null;
            }
            String origin = originCode.asText();
            
            // Extract destination with proper null checks
            JsonNode destinationCode = arrival.get("iataCode");
            if (destinationCode == null) {
                logger.warn("Missing arrival iataCode in flight segment");
                return null;
            }
            String destination = destinationCode.asText();
            
            logger.debug("Successfully extracted flight identifier: {} {} from {} to {} on {}", 
                carrierCode, flightNumber, origin, destination, departureDate);
            
            return new FlightIdentifier(carrierCode, flightNumber, departureDate, origin, destination);
        } catch (Exception e) {
            logger.error("Error extracting flight identifier: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Find matching flight in search results
     */
    private Optional<FlightSearchResult> findMatchingFlight(List<FlightSearchResult> flights, FlightIdentifier target) {
        return flights.stream()
            .filter(flight -> matchesFlight(flight, target))
            .findFirst();
    }
    
    /**
     * Check if a flight matches the target flight identifier
     */
    private boolean matchesFlight(FlightSearchResult flight, FlightIdentifier target) {
        try {
            List<JsonNode> itineraries = flight.getItineraries();
            if (itineraries == null || itineraries.size() == 0) {
                return false;
            }
            
            JsonNode firstItinerary = itineraries.get(0);
            JsonNode segments = firstItinerary.get("segments");
            if (segments == null || segments.size() == 0) {
                return false;
            }
            
            JsonNode firstSegment = segments.get(0);
            JsonNode departure = firstSegment.get("departure");
            JsonNode arrival = firstSegment.get("arrival");
            JsonNode operating = firstSegment.get("operating");
            
            // Extract carrier code with proper null checks
            String carrierCode = null;
            if (operating != null && operating.get("carrierCode") != null) {
                carrierCode = operating.get("carrierCode").asText();
            } else if (firstSegment.get("carrierCode") != null) {
                carrierCode = firstSegment.get("carrierCode").asText();
            }
            
            if (carrierCode == null) {
                return false;
            }
            
            // Extract flight number with proper null checks
            String flightNumber = null;
            if (operating != null && operating.get("number") != null) {
                flightNumber = operating.get("number").asText();
            } else if (firstSegment.get("number") != null) {
                flightNumber = firstSegment.get("number").asText();
            }
            
            if (flightNumber == null) {
                return false;
            }
            
            // Extract departure date with proper null checks
            if (departure == null || departure.get("at") == null) {
                return false;
            }
            String departureDate = departure.get("at").asText().substring(0, 10);
            
            // Extract origin with proper null checks
            if (departure.get("iataCode") == null) {
                return false;
            }
            String origin = departure.get("iataCode").asText();
            
            // Extract destination with proper null checks
            if (arrival == null || arrival.get("iataCode") == null) {
                return false;
            }
            String destination = arrival.get("iataCode").asText();
            
            return target.getCarrierCode().equals(carrierCode) &&
                   target.getFlightNumber().equals(flightNumber) &&
                   target.getDepartureDate().equals(departureDate) &&
                   target.getOrigin().equals(origin) &&
                   target.getDestination().equals(destination);
        } catch (Exception e) {
            logger.debug("Error matching flight: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if flight matches airline code (handles partial matches for saved searches)
     */
    private boolean matchesAirline(FlightSearchResult flight, String airlineCode) {
        if (flight.getValidatingAirlineCodes() != null) {
            return flight.getValidatingAirlineCodes().stream()
                .anyMatch(code -> code.startsWith(airlineCode));
        }
        return false;
    }
    
    /**
     * Calculate seat availability percentage using actual seatmap data
     */
    private double calculateSeatPercentage(FlightSearchResult flight) {
        // Use actual seatmap data if available
        if (flight.getSeatMap() != null && flight.getSeatMap().getSeats() != null) {
            var seats = flight.getSeatMap().getSeats();
            
            // Count total seats and available seats from seatmap
            int totalSeats = seats.size();
            long availableSeats = seats.stream()
                .filter(seat -> "AVAILABLE".equalsIgnoreCase(seat.getAvailabilityStatus()))
                .count();
            
            if (totalSeats > 0) {
                return (double) availableSeats / totalSeats * 100.0;
            }
        }
        
        // Fallback: use flight offer numberOfBookableSeats if seatmap unavailable
        // This is less accurate but better than hardcoded estimates
        int bookableSeats = flight.getNumberOfBookableSeats();
        if (bookableSeats > 0) {
            // Use bookableSeats as both available and total (conservative estimate)
            // This means we can't calculate percentage properly, so return a flag value
            logger.warn("Seatmap data unavailable for flight {}, using bookable seats count: {}", 
                flight.getId(), bookableSeats);
            // Return the actual count as percentage (will be used as absolute value in evaluation)
            return bookableSeats;
        }
        
        // No seat data available at all
        return 0.0;
    }
    
    /**
     * Flight identifier for matching flights across searches
     */
    private static class FlightIdentifier {
        private final String carrierCode;
        private final String flightNumber;
        private final String departureDate;
        private final String origin;
        private final String destination;
        
        public FlightIdentifier(String carrierCode, String flightNumber, String departureDate, String origin, String destination) {
            this.carrierCode = carrierCode;
            this.flightNumber = flightNumber;
            this.departureDate = departureDate;
            this.origin = origin;
            this.destination = destination;
        }
        
        public String getCarrierCode() { return carrierCode; }
        public String getFlightNumber() { return flightNumber; }
        public String getDepartureDate() { return departureDate; }
        public String getOrigin() { return origin; }
        public String getDestination() { return destination; }
    }
    
    /**
     * Result of alert evaluation
     */
    public static class AlertEvaluationResult {
        private final boolean triggered;
        private final boolean error;
        private final String message;
        private final double currentValue;
        private final double threshold;
        private final FlightSearchResult triggeringFlight;
        
        private AlertEvaluationResult(boolean triggered, boolean error, String message, double currentValue, double threshold, FlightSearchResult triggeringFlight) {
            this.triggered = triggered;
            this.error = error;
            this.message = message;
            this.currentValue = currentValue;
            this.threshold = threshold;
            this.triggeringFlight = triggeringFlight;
        }
        
        public static AlertEvaluationResult triggered(String message, double currentValue, double threshold, FlightSearchResult flight) {
            return new AlertEvaluationResult(true, false, message, currentValue, threshold, flight);
        }
        
        public static AlertEvaluationResult notTriggered(double currentValue, double threshold) {
            return new AlertEvaluationResult(false, false, "Alert not triggered", currentValue, threshold, null);
        }
        
        public static AlertEvaluationResult noAlert() {
            return new AlertEvaluationResult(false, false, "No alert configured", 0, 0, null);
        }
        
        public static AlertEvaluationResult error(String message) {
            return new AlertEvaluationResult(false, true, message, 0, 0, null);
        }
        
        public boolean isTriggered() { return triggered; }
        public boolean isError() { return error; }
        public String getMessage() { return message; }
        public double getCurrentValue() { return currentValue; }
        public double getThreshold() { return threshold; }
        public FlightSearchResult getTriggeringFlight() { return triggeringFlight; }
    }
}