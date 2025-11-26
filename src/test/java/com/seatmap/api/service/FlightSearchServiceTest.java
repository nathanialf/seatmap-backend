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
        request.setAirlineCode("AA");
        request.setFlightNumber("123");
        request.setMaxResults(5);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 2);
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", "AA", "123", 5, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(2, response.getData().size()); // Only Amadeus results
        assertEquals("AMADEUS", response.getMeta().getSources());
        
        verify(mockAmadeusService).searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", "AA", "123", 5, 0, false);
        // Sabre is disabled, so no longer called
        verify(mockSabreService, never()).searchFlightsWithSeatmaps(any(), any(), any(), any(), any(), any());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithParameters_ShouldReturnMeshedResults() throws Exception {
        // Arrange
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 2);
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(2, response.getData().size()); // Only Amadeus results
        assertEquals(2, response.getMeta().getCount());
        assertEquals("AMADEUS", response.getMeta().getSources());
        
        // Verify all results are from Amadeus
        assertEquals("AMADEUS", response.getData().get(0).getDataSource());
        assertEquals("AMADEUS", response.getData().get(1).getDataSource());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithAmadeusException_ShouldReturnEmptyResults() throws Exception {
        // Arrange - Since Sabre is disabled, Amadeus errors result in empty results
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false))
            .thenThrow(new RuntimeException("Amadeus API error"));
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(0, response.getData().size());
        assertEquals("AMADEUS", response.getMeta().getSources());
    }
    
    
    @Test
    void searchFlightsWithSeatmaps_WithAmadeusServiceException_ShouldReturnEmptyResults() throws Exception {
        // Arrange - Only Amadeus is used, so when it fails we get empty results
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false))
            .thenThrow(new RuntimeException("Amadeus API error"));
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10
        );
        
        // Assert - Service returns empty results when Amadeus API fails, doesn't throw exception
        assertNotNull(response);
        assertEquals(0, response.getData().size());
        assertEquals(0, response.getMeta().getCount());
        assertEquals("AMADEUS", response.getMeta().getSources());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithAmadeusResults_ShouldReturnAmadeusOnly() throws Exception {
        // Arrange - Only Amadeus is called, no deduplication needed
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 1);
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10
        );
        
        // Assert - Should have 1 result from Amadeus
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("AMADEUS", response.getData().get(0).getDataSource());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithMaxResults_ShouldLimitResults() throws Exception {
        // Arrange
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 3);
        List<FlightSearchResult> sabreMockResults = createMockFlightResults("SABRE", 5);
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 3, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act - Limit to 3 results
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 3
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(3, response.getData().size());
        assertEquals(3, response.getMeta().getCount());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithNullMaxResults_ShouldDefaultTo10() throws Exception {
        // Arrange - The service defaults null to 10 internally, so APIs are called with 10
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 10);
        List<FlightSearchResult> sabreMockResults = createMockFlightResults("SABRE", 0);
        
        // Note: The service converts null maxResults to 10 before calling the APIs
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, null, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, null, null
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(10, response.getData().size()); // Default limit of 10
        assertEquals(10, response.getMeta().getCount());
    }
    
    @Test
    void searchFlightsWithSeatmaps_WithNoResults_ShouldReturnEmptyResponse() throws Exception {
        // Arrange
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false))
            .thenReturn(new ArrayList<>());
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(
            "LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10
        );
        
        // Assert
        assertNotNull(response);
        assertEquals(0, response.getData().size());
        assertEquals(0, response.getMeta().getCount());
        assertEquals("AMADEUS", response.getMeta().getSources());
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
    
    @Test
    void searchFlightsWithSeatmaps_WithIncludeRawFlightOfferFalse_ShouldNotIncludeRawData() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setIncludeRawFlightOffer(false);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResultsWithRawData("AMADEUS", 1, false);
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertNull(response.getData().get(0).getRawFlightOffer());
    }

    @Test
    void searchFlightsWithSeatmaps_WithIncludeRawFlightOfferTrue_ShouldIncludeRawData() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setIncludeRawFlightOffer(true);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResultsWithRawData("AMADEUS", 1, true);
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, true))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertNotNull(response.getData().get(0).getRawFlightOffer());
    }

    @Test
    void searchFlightsWithSeatmaps_WithIncludeRawFlightOfferNull_ShouldDefaultToFalse() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setIncludeRawFlightOffer(null);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResultsWithRawData("AMADEUS", 1, false);
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertNull(response.getData().get(0).getRawFlightOffer());
    }

    // Helper methods for raw data tests
    
    private List<FlightSearchResult> createMockFlightResultsWithRawData(String dataSource, int count, boolean includeRaw) {
        List<FlightSearchResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String carrier = dataSource.equals("AMADEUS") ? "AA" : "DL";
            String flightNumber = String.valueOf(100 + i);
            results.add(createMockFlightResultWithRawData(dataSource, carrier, flightNumber, "LAX", "JFK", "2024-06-15T10:00:00", includeRaw));
        }
        return results;
    }
    
    private FlightSearchResult createMockFlightResultWithRawData(String dataSource, String carrier, String number, 
                                                                String origin, String destination, String departureTime, boolean includeRaw) {
        // Create original flight offer JSON
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", dataSource.toLowerCase() + "-" + carrier + number);
        flightOffer.put("dataSource", dataSource);
        flightOffer.put("source", "GDS");
        flightOffer.put("type", "flight-offer");
        
        // Create itinerary
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
        
        ArrayNode itineraries = objectMapper.createArrayNode();
        itineraries.add(itinerary);
        flightOffer.set("itineraries", itineraries);
        
        // Create result using constructor that includes raw data option
        return new FlightSearchResult(flightOffer, null, false, null, includeRaw);
    }

    // Pagination Integration Tests
    @Test
    void searchFlightsWithSeatmaps_WithOffset_ShouldPassOffsetToAmadeus() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setMaxResults(15);
        request.setOffset(30);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 15);
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 15, 30, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(15, response.getData().size());
        assertEquals("AMADEUS", response.getMeta().getSources());
        
        // Verify offset was passed to Amadeus service
        verify(mockAmadeusService).searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 15, 30, false);
    }

    @Test
    void searchFlightsWithSeatmaps_WithNullOffset_ShouldDefaultToZero() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setMaxResults(10);
        request.setOffset(null); // Explicitly null
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 10);
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(10, response.getData().size());
        
        // Verify offset defaulted to 0
        verify(mockAmadeusService).searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 0, false);
    }

    @Test
    void searchFlightsWithSeatmaps_ShouldIncludePaginationMetadata() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setMaxResults(10);
        request.setOffset(20);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 10); // Full page
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 20, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getMeta());
        assertNotNull(response.getMeta().getPagination());
        
        FlightSearchResponse.PaginationInfo pagination = response.getMeta().getPagination();
        assertEquals(20, pagination.getOffset());
        assertEquals(10, pagination.getLimit());
        assertEquals(-1, pagination.getTotal()); // Unknown total from Amadeus
        assertTrue(pagination.isHasNext());  // Full page indicates more results
        assertTrue(pagination.isHasPrevious()); // Offset > 0 indicates previous pages
    }

    @Test
    void searchFlightsWithSeatmaps_PartialPage_ShouldIndicateNoMoreResults() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setMaxResults(10);
        request.setOffset(20);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 5); // Partial page
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 20, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        FlightSearchResponse.PaginationInfo pagination = response.getMeta().getPagination();
        assertEquals(20, pagination.getOffset());
        assertEquals(10, pagination.getLimit());
        assertFalse(pagination.isHasNext());  // Partial page indicates no more results
        assertTrue(pagination.isHasPrevious());
    }

    @Test
    void searchFlightsWithSeatmaps_FirstPage_ShouldIndicateNoPreviousResults() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setMaxResults(15);
        request.setOffset(0);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 15); // Full first page
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 15, 0, false))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        FlightSearchResponse.PaginationInfo pagination = response.getMeta().getPagination();
        assertEquals(0, pagination.getOffset());
        assertEquals(15, pagination.getLimit());
        assertTrue(pagination.isHasNext());   // Full page indicates more results
        assertFalse(pagination.isHasPrevious()); // First page has no previous
    }

    @Test
    void searchFlightsWithSeatmaps_EmptyResults_ShouldHaveCorrectPagination() throws Exception {
        // Arrange
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setMaxResults(10);
        request.setOffset(50);
        
        List<FlightSearchResult> emptyResults = new ArrayList<>();
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 10, 50))
            .thenReturn(emptyResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(0, response.getData().size());
        
        FlightSearchResponse.PaginationInfo pagination = response.getMeta().getPagination();
        assertEquals(50, pagination.getOffset());
        assertEquals(10, pagination.getLimit());
        assertFalse(pagination.isHasNext());  // No results means no more
        assertTrue(pagination.isHasPrevious()); // Offset > 0 means previous pages exist
    }

    @Test
    void searchFlightsWithSeatmaps_BoundaryOffsetValues_ShouldWorkCorrectly() throws Exception {
        // Test maximum offset (100)
        FlightSearchRequest request = new FlightSearchRequest("LAX", "JFK", "2024-06-15", "ECONOMY");
        request.setMaxResults(20);
        request.setOffset(100);
        
        List<FlightSearchResult> amadeusMockResults = createMockFlightResults("AMADEUS", 5); // Partial last page
        
        when(mockAmadeusService.searchFlightsWithBatchSeatmaps("LAX", "JFK", "2024-06-15", "ECONOMY", null, null, 20, 100))
            .thenReturn(amadeusMockResults);
        
        // Act
        FlightSearchResponse response = flightSearchService.searchFlightsWithSeatmaps(request);
        
        // Assert
        assertNotNull(response);
        FlightSearchResponse.PaginationInfo pagination = response.getMeta().getPagination();
        assertEquals(100, pagination.getOffset()); // Maximum offset
        assertEquals(20, pagination.getLimit());   // Maximum page size
        assertFalse(pagination.isHasNext());       // Partial page at max offset
        assertTrue(pagination.isHasPrevious());    // Has previous pages
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