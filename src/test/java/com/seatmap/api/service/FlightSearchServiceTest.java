package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.api.model.FlightSearchResponse;
import com.seatmap.common.exception.SeatmapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightSearchServiceTest {
    
    @Mock
    private AmadeusService mockAmadeusService;
    
    @Mock
    private SabreService mockSabreService;
    
    private FlightSearchService flightSearchService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        flightSearchService = new FlightSearchService(mockAmadeusService, mockSabreService);
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void constructor_ShouldInitializeServices() {
        // Assert
        assertNotNull(flightSearchService);
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithFlightSearchRequest_ShouldCallParameterMethod() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setFlightNumber("AA123");
        request.setMaxResults(5);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 2);
        List<FlightSearchResult> sabreMockResults = createMockFlightResults("SABRE", 1);
        
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", "AA123", 5))
            .thenReturn(amadeusMockResults);
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", "AA123", 5))
            .thenReturn(sabreMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(3, response.getData().size());
        assertEquals("AMADEUS,SABRE", response.getMeta().getSources());
        
        verify(mockAmadeusService).searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", "AA123", 5);
        verify(mockSabreService).searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", "AA123", 5);
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithParameters_ShouldReturnMeshedResults() throws Exception {
        // Arrange
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 2);
        List<FlightSearchResult> sabreMockResults = createMockFlightResults("SABRE", 2);
        
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenReturn(amadeusMockResults);
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenReturn(sabreMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, 10
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(4, response.getData().size());
        assertEquals(4, response.getMeta().getCount());
        assertEquals("AMADEUS,SABRE", response.getMeta().getSources());
        
        // Verify Amadeus results come first (higher priority)
        assertEquals("AMADEUS", response.getData().get(0).getDataSource());
        assertEquals("AMADEUS", response.getData().get(1).getDataSource());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithAmadeusException_ShouldReturnSabreResults() throws Exception {
        // Arrange
        List<FlightSearchResult> sabreMockResults = createMockFlightResults("SABRE", 1);
        
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenThrow(new RuntimeException("Amadeus API error"));
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenReturn(sabreMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, 10
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("SABRE", response.getData().get(0).getDataSource());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithSabreException_ShouldReturnAmadeusResults() throws Exception {
        // Arrange
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 1);
        
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenReturn(amadeusMockResults);
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenThrow(new RuntimeException("Sabre API error"));
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, 10
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("AMADEUS", response.getData().get(0).getDataSource());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithBothServicesException_ShouldReturnEmptyResults() throws Exception {
        // Arrange - The service catches exceptions and returns empty lists, doesn't throw
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenThrow(new RuntimeException("Amadeus API error"));
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenThrow(new RuntimeException("Sabre API error"));
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, 10
        );
        
        // Assert - Service returns empty results when both APIs fail, doesn't throw exception
        assertNotNull(response);
        assertEquals(0, response.getData().size());
        assertEquals(0, response.getMeta().getCount());
        assertEquals("AMADEUS,SABRE", response.getMeta().getSources());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithDuplicateFlights_ShouldDeduplicateResults() throws Exception {
        // Arrange - Create flights with same key (same flight)
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 1);
        List<FlightSearchResult> sabreMockResults = createMockFlightResults("SABRE", 1);
        
        // Make the Sabre result have the same flight key as Amadeus result
        FlightSearchResult sabreResult = sabreMockResults.get(0);
        FlightSearchResult amadeusResult = amadeusMockResults.get(0);
        // Copy the itinerary from Amadeus to Sabre to create duplicate
        sabreResult.setItineraries(amadeusResult.getItineraries());
        
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenReturn(amadeusMockResults);
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenReturn(sabreMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, 10
        );
        
        // Assert - Should only have 1 result (Amadeus preferred)
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("AMADEUS", response.getData().get(0).getDataSource());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithMaxResults_ShouldLimitResults() throws Exception {
        // Arrange
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 5);
        List<FlightSearchResult> sabreMockResults = createMockFlightResults("SABRE", 5);
        
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 3))
            .thenReturn(amadeusMockResults);
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 3))
            .thenReturn(sabreMockResults);
        
        // Act - Limit to 3 results
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, 3
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(3, response.getData().size());
        assertEquals(3, response.getMeta().getCount());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithNullMaxResults_ShouldDefaultTo10() throws Exception {
        // Arrange - The service defaults null to 10 internally, so APIs are called with 10
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 15);
        List<FlightSearchResult> sabreMockResults = createMockFlightResults("SABRE", 0);
        
        // Note: The service converts null maxResults to 10 before calling the APIs
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null))
            .thenReturn(amadeusMockResults);
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null))
            .thenReturn(sabreMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, null
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(10, response.getData().size()); // Default limit of 10
        assertEquals(10, response.getMeta().getCount());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithNoResults_ShouldReturnEmptyResponse() throws Exception {
        // Arrange
        when(mockAmadeusService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenReturn(new ArrayList<>());
        when(mockSabreService.searchFlightsWithSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, 10))
            .thenReturn(new ArrayList<>());
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, 10
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(0, response.getData().size());
        assertEquals(0, response.getMeta().getCount());
        assertEquals("AMADEUS,SABRE", response.getMeta().getSources());
    }
    
    @Test
    void createFlightKey_WithValidFlightData_ShouldReturnCorrectKey() {
        // Arrange
        FlightSearchResult flight = createMockFlightResult("AMADEUS", "AA", "123", "LAX", "JFK", "2024-06-15T10:00:00");
        
        // Act
        String key = invokeCreateFlightKey(flight);
        
        // Assert
        assertEquals("AA123LAXJFK2024-06-15", key);
    }
    
    @Test
    void createFlightKey_WithMissingItinerary_ShouldReturnEmptyKey() {
        // Arrange
        FlightSearchResult flight = new FlightSearchResult();
        flight.setDataSource("AMADEUS");
        
        // Act
        String key = invokeCreateFlightKey(flight);
        
        // Assert
        assertEquals("", key);
    }
    
    @Test
    void createFlightKey_WithEmptyItinerary_ShouldReturnEmptyKey() {
        // Arrange
        FlightSearchResult flight = new FlightSearchResult();
        flight.setDataSource("AMADEUS");
        flight.setItineraries(new ArrayList<>());
        
        // Act
        String key = invokeCreateFlightKey(flight);
        
        // Assert
        assertEquals("", key);
    }
    
    @Test
    void createFlightKey_WithMissingSegments_ShouldReturnEmptyKey() {
        // Arrange
        FlightSearchResult flight = new FlightSearchResult();
        flight.setDataSource("AMADEUS");
        ObjectNode itinerary = objectMapper.createObjectNode();
        List<JsonNode> itineraries = new ArrayList<>();
        itineraries.add(itinerary);
        flight.setItineraries(itineraries);
        
        // Act
        String key = invokeCreateFlightKey(flight);
        
        // Assert
        assertEquals("", key);
    }
    
    // Helper methods
    
    private List<FlightSearchResult> createMockFlightResults(String dataSource, int count) {
        List<FlightSearchResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String carrier = dataSource.equals("AMADEUS") ? "AA" : "DL";
            String flightNumber = String.valueOf(100 + i);
            results.add(createMockFlightResult(dataSource, carrier, flightNumber, "LAX", "JFK", "2024-06-15T10:00:00"));
        }
        return results;
    }
    
    private FlightSearchResult createMockFlightResult(String dataSource, String carrier, String number, 
                                                     String origin, String destination, String departureTime) {
        FlightSearchResult result = new FlightSearchResult();
        result.setDataSource(dataSource);
        
        // Create itinerary with segment
        ObjectNode itinerary = objectMapper.createObjectNode();
        ArrayNode segments = objectMapper.createArrayNode();
        ObjectNode segment = objectMapper.createObjectNode();
        
        segment.put("carrierCode", carrier);
        segment.put("number", number);
        
        ObjectNode departure = objectMapper.createObjectNode();
        departure.put("iataCode", origin);
        departure.put("at", departureTime);
        segment.set("departure", departure);
        
        ObjectNode arrival = objectMapper.createObjectNode();
        arrival.put("iataCode", destination);
        arrival.put("at", departureTime.replace("10:00:00", "13:00:00"));
        segment.set("arrival", arrival);
        
        segments.add(segment);
        itinerary.set("segments", segments);
        
        List<JsonNode> itineraries = new ArrayList<>();
        itineraries.add(itinerary);
        result.setItineraries(itineraries);
        
        return result;
    }
    
    // Helper method to invoke private createFlightKey method using reflection
    private String invokeCreateFlightKey(FlightSearchResult result) {
        try {
            var method = FlightSearchService.class.getDeclaredMethod("createFlightKey", FlightSearchResult.class);
            method.setAccessible(true);
            return (String) method.invoke(flightSearchService, result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke createFlightKey", e);
        }
    }
}