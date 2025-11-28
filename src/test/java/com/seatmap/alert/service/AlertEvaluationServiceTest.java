package com.seatmap.alert.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.api.model.FlightSearchResponse;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.common.model.Bookmark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertEvaluationServiceTest {
    
    private AlertEvaluationService alertEvaluationService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        alertEvaluationService = new AlertEvaluationService();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testEvaluateAlert_NoAlertConfig() {
        // Given
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        // No alert config set
        
        FlightSearchResponse response = new FlightSearchResponse();
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        assertFalse(result.isTriggered());
        assertFalse(result.isError());
    }
    
    @Test
    void testEvaluateAlert_AlertDisabled() {
        // Given
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setAlertConfig(new Bookmark.AlertConfig(null)); // null threshold means disabled
        
        FlightSearchResponse response = new FlightSearchResponse();
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        assertFalse(result.isTriggered());
        assertFalse(result.isError());
    }
    
    @Test
    void testEvaluateBookmarkAlert_BelowThreshold() {
        // Given
        Bookmark bookmark = createBookmarkWithFlightData();
        bookmark.setAlertConfig(new Bookmark.AlertConfig(10.0)); // Trigger below 10 seats
        
        FlightSearchResponse response = createFlightSearchResponse(5); // 5 seats available
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        assertTrue(result.isTriggered());
        assertFalse(result.isError());
        assertEquals(5.0, result.getCurrentValue());
        assertEquals(10.0, result.getThreshold());
        assertTrue(result.getMessage().contains("below threshold"));
    }
    
    @Test
    void testEvaluateBookmarkAlert_AboveThreshold() {
        // Given
        Bookmark bookmark = createBookmarkWithFlightData();
        bookmark.setAlertConfig(new Bookmark.AlertConfig(10.0)); // Trigger below 10 seats
        
        FlightSearchResponse response = createFlightSearchResponse(15); // 15 seats available
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        assertFalse(result.isTriggered());
        assertFalse(result.isError());
        assertEquals(15.0, result.getCurrentValue());
        assertEquals(10.0, result.getThreshold());
    }
    
    @Test
    void testEvaluateSavedSearchAlert_AboveThreshold() {
        // Given
        Bookmark bookmark = createSavedSearchBookmark();
        bookmark.setAlertConfig(new Bookmark.AlertConfig(15.0)); // Trigger above 15% availability
        
        FlightSearchResponse response = createFlightSearchResponse(30); // 30 seats = 20% of 150
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        assertTrue(result.isTriggered());
        assertFalse(result.isError());
        assertEquals(30.0, result.getCurrentValue());
        assertEquals(15.0, result.getThreshold());
        assertTrue(result.getMessage().contains("above threshold"));
    }
    
    @Test
    void testEvaluateSavedSearchAlert_BelowThreshold() {
        // Given
        Bookmark bookmark = createSavedSearchBookmark();
        bookmark.setAlertConfig(new Bookmark.AlertConfig(15.0)); // Trigger above 15% availability
        
        FlightSearchResponse response = createFlightSearchResponse(10); // 10 seats = 6.7% of 150
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        assertFalse(result.isTriggered());
        assertFalse(result.isError());
        assertEquals(0.0, result.getCurrentValue()); // No matching flights above threshold
        assertEquals(15.0, result.getThreshold());
    }
    
    @Test
    void testEvaluateAlert_InvalidFlightData() {
        // Given
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setFlightOfferData("invalid json");
        bookmark.setAlertConfig(new Bookmark.AlertConfig(10.0));
        
        FlightSearchResponse response = createFlightSearchResponse(5);
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        assertFalse(result.isTriggered());
        assertTrue(result.isError());
        assertTrue(result.getMessage().contains("Error parsing flight data"));
    }
    
    @Test
    void testEvaluateAlert_FlightNotFound() {
        // Given
        Bookmark bookmark = createBookmarkWithFlightData();
        bookmark.setAlertConfig(new Bookmark.AlertConfig(10.0));
        
        // Create response with different flight
        FlightSearchResponse response = createFlightSearchResponseWithDifferentFlight();
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        assertFalse(result.isTriggered());
        assertTrue(result.isError());
        assertTrue(result.getMessage().contains("Flight not found"));
    }
    
    @Test
    void testSavedSearchAlert_WithAirlineFilter() {
        // Given
        Bookmark bookmark = createSavedSearchBookmark();
        bookmark.setAirlineCode("UA"); // United Airlines filter
        bookmark.setAlertConfig(new Bookmark.AlertConfig(15.0));
        
        // Create response with mixed airlines
        FlightSearchResponse response = createMixedAirlineFlightResponse();
        
        // When
        AlertEvaluationService.AlertEvaluationResult result = 
            alertEvaluationService.evaluateAlert(bookmark, response);
        
        // Then
        // Should trigger only for UA flights above threshold
        boolean hasUAFlightAboveThreshold = response.getData().stream()
            .anyMatch(flight -> flight.getValidatingAirlineCodes() != null &&
                              flight.getValidatingAirlineCodes().contains("UA") &&
                              flight.getNumberOfBookableSeats() > 22); // 15% of 150
        
        assertEquals(hasUAFlightAboveThreshold, result.isTriggered());
    }
    
    // Helper methods
    
    private Bookmark createBookmarkWithFlightData() {
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("Test Flight");
        
        // Create realistic flight offer data
        ObjectNode flightData = objectMapper.createObjectNode();
        ObjectNode itinerary = objectMapper.createObjectNode();
        ObjectNode segment = objectMapper.createObjectNode();
        ObjectNode departure = objectMapper.createObjectNode();
        ObjectNode arrival = objectMapper.createObjectNode();
        ObjectNode operating = objectMapper.createObjectNode();
        
        departure.put("iataCode", "LAX");
        departure.put("at", "2024-12-15T10:00:00");
        arrival.put("iataCode", "JFK");
        arrival.put("at", "2024-12-15T18:00:00");
        operating.put("carrierCode", "AA");
        operating.put("number", "123");
        
        segment.set("departure", departure);
        segment.set("arrival", arrival);
        segment.set("operating", operating);
        
        itinerary.set("segments", objectMapper.createArrayNode().add(segment));
        flightData.set("itineraries", objectMapper.createArrayNode().add(itinerary));
        
        bookmark.setFlightOfferData(flightData.toString());
        return bookmark;
    }
    
    private Bookmark createSavedSearchBookmark() {
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-search");
        bookmark.setTitle("Test Search");
        bookmark.setOrigin("LAX");
        bookmark.setDestination("JFK");
        bookmark.setDepartureDate("2024-12-15");
        bookmark.setTravelClass("ECONOMY");
        return bookmark;
    }
    
    private FlightSearchResponse createFlightSearchResponse(int numberOfSeats) {
        FlightSearchResult flight = new FlightSearchResult();
        flight.setId("test-flight");
        flight.setDataSource("amadeus");
        flight.setNumberOfBookableSeats(numberOfSeats);
        
        // Set itineraries for matching
        ObjectNode itinerary = objectMapper.createObjectNode();
        ObjectNode segment = objectMapper.createObjectNode();
        ObjectNode departure = objectMapper.createObjectNode();
        ObjectNode arrival = objectMapper.createObjectNode();
        ObjectNode operating = objectMapper.createObjectNode();
        
        departure.put("iataCode", "LAX");
        departure.put("at", "2024-12-15T10:00:00");
        arrival.put("iataCode", "JFK");
        arrival.put("at", "2024-12-15T18:00:00");
        operating.put("carrierCode", "AA");
        operating.put("number", "123");
        
        segment.set("departure", departure);
        segment.set("arrival", arrival);
        segment.set("operating", operating);
        
        itinerary.set("segments", objectMapper.createArrayNode().add(segment));
        List<JsonNode> itineraries = Arrays.asList(itinerary);
        flight.setItineraries(itineraries);
        
        FlightSearchResponse response = new FlightSearchResponse();
        response.setData(Arrays.asList(flight));
        
        return response;
    }
    
    private FlightSearchResponse createFlightSearchResponseWithDifferentFlight() {
        FlightSearchResult flight = new FlightSearchResult();
        flight.setId("different-flight");
        flight.setDataSource("amadeus");
        flight.setNumberOfBookableSeats(5);
        
        // Set different route
        ObjectNode itinerary = objectMapper.createObjectNode();
        ObjectNode segment = objectMapper.createObjectNode();
        ObjectNode departure = objectMapper.createObjectNode();
        ObjectNode arrival = objectMapper.createObjectNode();
        ObjectNode operating = objectMapper.createObjectNode();
        
        departure.put("iataCode", "SFO");
        departure.put("at", "2024-12-15T10:00:00");
        arrival.put("iataCode", "ORD");
        arrival.put("at", "2024-12-15T18:00:00");
        operating.put("carrierCode", "UA");
        operating.put("number", "456");
        
        segment.set("departure", departure);
        segment.set("arrival", arrival);
        segment.set("operating", operating);
        
        itinerary.set("segments", objectMapper.createArrayNode().add(segment));
        List<JsonNode> itineraries = Arrays.asList(itinerary);
        flight.setItineraries(itineraries);
        
        FlightSearchResponse response = new FlightSearchResponse();
        response.setData(Arrays.asList(flight));
        
        return response;
    }
    
    private FlightSearchResponse createMixedAirlineFlightResponse() {
        // Create UA flight with high availability
        FlightSearchResult uaFlight = new FlightSearchResult();
        uaFlight.setId("ua-flight");
        uaFlight.setDataSource("amadeus");
        uaFlight.setNumberOfBookableSeats(30); // 20% availability
        uaFlight.setValidatingAirlineCodes(Arrays.asList("UA"));
        
        // Create AA flight with low availability
        FlightSearchResult aaFlight = new FlightSearchResult();
        aaFlight.setId("aa-flight");
        aaFlight.setDataSource("amadeus");
        aaFlight.setNumberOfBookableSeats(10); // 6.7% availability
        aaFlight.setValidatingAirlineCodes(Arrays.asList("AA"));
        
        FlightSearchResponse response = new FlightSearchResponse();
        response.setData(Arrays.asList(uaFlight, aaFlight));
        
        return response;
    }
}