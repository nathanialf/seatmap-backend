package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.api.exception.SeatmapApiException;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.api.model.SeatMapData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmadeusServiceTest {
    
    private AmadeusService amadeusService;
    private HttpClient mockHttpClient;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockHttpClient = mock(HttpClient.class);
        
        // Environment variables are already set in build.gradle
        amadeusService = new AmadeusService();
        
        // Use reflection to inject mock HttpClient
        try {
            var httpClientField = AmadeusService.class.getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            httpClientField.set(amadeusService, mockHttpClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock HttpClient", e);
        }
    }
    
    
    @Test
    void getSeatMap_WithValidParameters_ReturnsJsonNode() throws Exception {
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock flight offers response
        HttpResponse<String> flightOffersResponse = mock(HttpResponse.class);
        when(flightOffersResponse.statusCode()).thenReturn(200);
        String flightOffersJson = "{\"data\":[{\"id\":\"offer1\",\"type\":\"flight-offer\"}]}";
        when(flightOffersResponse.body()).thenReturn(flightOffersJson);
        
        // Mock seat map response
        HttpResponse<String> seatMapResponse = mock(HttpResponse.class);
        when(seatMapResponse.statusCode()).thenReturn(200);
        String seatMapJson = "{\"data\":[{\"type\":\"seat-map\",\"flightNumber\":\"AA123\"}]}";
        when(seatMapResponse.body()).thenReturn(seatMapJson);
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse) // First call for token
            .thenReturn(flightOffersResponse) // Second call for flight offers
            .thenReturn(seatMapResponse); // Third call for seat map
        
        JsonNode result = amadeusService.getSeatMap("AA123", "2024-12-01", "LAX", "JFK");
        
        assertNotNull(result);
        assertTrue(result.has("data"));
        verify(mockHttpClient, times(3)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }
    
    @Test
    void getSeatMap_WithInvalidFlightNumber_ThrowsException() throws Exception {
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock error response
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(404);
        when(errorResponse.body()).thenReturn("{\"error\":\"Flight not found\"}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse)
            .thenReturn(errorResponse);
        
        assertThrows(SeatmapApiException.class, () -> 
            amadeusService.getSeatMap("INVALID", "2024-12-01", "LAX", "JFK"));
    }
    
    @Test
    void getSeatMap_WithNetworkError_ThrowsException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenThrow(new IOException("Network error"));
        
        assertThrows(SeatmapApiException.class, () -> 
            amadeusService.getSeatMap("AA123", "2024-12-01", "LAX", "JFK"));
    }
    
    @Test
    void getSeatMap_WithTokenRefreshFailure_ThrowsException() throws Exception {
        // Mock failed token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(401);
        when(tokenResponse.body()).thenReturn("{\"error\":\"Unauthorized\"}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse);
        
        assertThrows(SeatmapApiException.class, () -> 
            amadeusService.getSeatMap("AA123", "2024-12-01", "LAX", "JFK"));
    }
    
    @Test
    void getSeatMap_WithSpecialCharactersInParameters_EncodesCorrectly() throws Exception {
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock flight offers response with data
        HttpResponse<String> flightOffersResponse = mock(HttpResponse.class);
        when(flightOffersResponse.statusCode()).thenReturn(200);
        String flightOffersJson = "{\"data\":[{\"id\":\"offer1\",\"type\":\"flight-offer\"}]}";
        when(flightOffersResponse.body()).thenReturn(flightOffersJson);
        
        // Mock seat map response
        HttpResponse<String> seatMapResponse = mock(HttpResponse.class);
        when(seatMapResponse.statusCode()).thenReturn(200);
        when(seatMapResponse.body()).thenReturn("{\"data\":[{\"type\":\"seat-map\"}]}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse)
            .thenReturn(flightOffersResponse)
            .thenReturn(seatMapResponse);
        
        // Test with special characters that need URL encoding
        assertDoesNotThrow(() -> 
            amadeusService.getSeatMap("AA 123", "2024-12-01", "LAX", "JFK"));
    }
    
    @Test
    void getSeatMap_WithExpiredToken_RefreshesTokenAutomatically() throws Exception {
        // First call - token response
        HttpResponse<String> tokenResponse1 = mock(HttpResponse.class);
        when(tokenResponse1.statusCode()).thenReturn(200);
        when(tokenResponse1.body()).thenReturn("{\"access_token\":\"token1\",\"expires_in\":1}"); // Expires in 1 second
        
        // Second call - new token response
        HttpResponse<String> tokenResponse2 = mock(HttpResponse.class);
        when(tokenResponse2.statusCode()).thenReturn(200);
        when(tokenResponse2.body()).thenReturn("{\"access_token\":\"token2\",\"expires_in\":3600}");
        
        // Flight offers responses
        HttpResponse<String> flightOffersResponse = mock(HttpResponse.class);
        when(flightOffersResponse.statusCode()).thenReturn(200);
        String flightOffersJson = "{\"data\":[{\"id\":\"offer1\",\"type\":\"flight-offer\"}]}";
        when(flightOffersResponse.body()).thenReturn(flightOffersJson);
        
        // Seat map responses
        HttpResponse<String> seatMapResponse = mock(HttpResponse.class);
        when(seatMapResponse.statusCode()).thenReturn(200);
        when(seatMapResponse.body()).thenReturn("{\"data\":[{\"type\":\"seat-map\"}]}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse1)
            .thenReturn(flightOffersResponse)
            .thenReturn(seatMapResponse)
            .thenReturn(tokenResponse2)  // Token refresh
            .thenReturn(flightOffersResponse)
            .thenReturn(seatMapResponse);
        
        // First call
        amadeusService.getSeatMap("AA123", "2024-12-01", "LAX", "JFK");
        
        // Wait for token to expire and make second call
        Thread.sleep(1100);
        amadeusService.getSeatMap("AA124", "2024-12-01", "LAX", "JFK");
        
        // Should have made 6 HTTP calls total (2 tokens + 2 flight offers + 2 seat maps)
        verify(mockHttpClient, times(6)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }
    
    @Test
    void convertToSeatMapData_WithValidResponse_ReturnsCompleteData() throws Exception {
        String seatMapJson = """
        {
            "data": [{
                "type": "seat-map",
                "number": "1",
                "carrierCode": "UA",
                "aircraft": {
                    "code": "73H"
                },
                "departure": {
                    "iataCode": "SFO",
                    "at": "2025-12-05T08:30:00"
                },
                "arrival": {
                    "iataCode": "CUN",
                    "at": "2025-12-05T16:45:00"
                },
                "decks": [{
                    "deckType": "MAIN",
                    "deckConfiguration": {
                        "width": 6,
                        "length": 30
                    },
                    "facilities": [{
                        "code": "LA",
                        "column": "3",
                        "row": "15"
                    }],
                    "seats": [{
                        "number": "12A",
                        "characteristicsCodes": ["9", "A", "CH", "K", "LS", "O", "Q", "W"],
                        "travelerPricing": [{
                            "travelerId": "1",
                            "seatAvailabilityStatus": "AVAILABLE",
                            "price": {
                                "currency": "USD",
                                "total": "25.00",
                                "base": "20.00",
                                "fees": [{
                                    "amount": "5.00",
                                    "type": "TICKETING"
                                }],
                                "taxes": [{
                                    "amount": "2.50",
                                    "code": "YQ"
                                }]
                            }
                        }]
                    }]
                }]
            }]
        }
        """;
        
        JsonNode seatMapResponse = objectMapper.readTree(seatMapJson);
        SeatMapData result = amadeusService.convertToSeatMapData(seatMapResponse);
        
        // Verify basic flight info
        assertNotNull(result);
        assertEquals("AMADEUS", result.getSource());
        assertNotNull(result.getFlight());
        assertEquals("1", result.getFlight().getNumber());
        assertEquals("UA", result.getFlight().getCarrierCode());
        assertEquals("SFO", result.getFlight().getDeparture().getIataCode());
        assertEquals("CUN", result.getFlight().getArrival().getIataCode());
        
        // Verify aircraft info
        assertNotNull(result.getAircraft());
        assertEquals("73H", result.getAircraft().getCode());
        
        // Verify decks
        assertNotNull(result.getDecks());
        assertEquals(1, result.getDecks().size());
        SeatMapData.SeatMapDeck deck = result.getDecks().get(0);
        assertEquals("MAIN", deck.getDeckType());
        assertNotNull(deck.getDeckConfiguration());
        
        // Verify facilities
        assertNotNull(deck.getFacilities());
        assertEquals(1, deck.getFacilities().size());
        // Facilities are stored as JsonNode
        assertNotNull(deck.getFacilities().get(0));
        
        // Verify seats
        assertNotNull(deck.getSeats());
        assertEquals(1, deck.getSeats().size());
        SeatMapData.Seat seat = deck.getSeats().get(0);
        assertEquals("12A", seat.getNumber());
        assertNotNull(seat.getCharacteristicsCodes());
        assertEquals(8, seat.getCharacteristicsCodes().size());
        assertTrue(seat.getCharacteristicsCodes().contains("9"));
        assertTrue(seat.getCharacteristicsCodes().contains("W"));
        
        // Verify seat pricing (stored as JsonNode in the model)
        assertNotNull(seat.getTravelerPricing());
        assertEquals(1, seat.getTravelerPricing().size());
    }
    
    @Test
    void convertToSeatMapData_WithNullResponse_ReturnsEmptyData() {
        SeatMapData result = amadeusService.convertToSeatMapData(null);
        
        assertNotNull(result);
        assertEquals("AMADEUS", result.getSource());
        assertNull(result.getFlight());
        assertNull(result.getAircraft());
        assertNull(result.getDecks());
        assertNull(result.getSeats());
        assertNull(result.getLayout());
    }
    
    @Test
    void convertToSeatMapData_WithEmptyResponse_ReturnsEmptyData() throws Exception {
        String emptyJson = "{}";
        JsonNode emptyResponse = objectMapper.readTree(emptyJson);
        
        SeatMapData result = amadeusService.convertToSeatMapData(emptyResponse);
        
        assertNotNull(result);
        assertEquals("AMADEUS", result.getSource());
        assertNull(result.getFlight());
        assertNull(result.getAircraft());
        assertNull(result.getDecks());
    }
    
    @Test
    void convertToSeatMapData_WithMissingDataField_ReturnsEmptyData() throws Exception {
        String noDataJson = """
        {
            "meta": {
                "count": 0
            }
        }
        """;
        JsonNode noDataResponse = objectMapper.readTree(noDataJson);
        
        SeatMapData result = amadeusService.convertToSeatMapData(noDataResponse);
        
        assertNotNull(result);
        assertEquals("AMADEUS", result.getSource());
        assertNull(result.getFlight());
    }
    
    @Test
    void convertToSeatMapData_WithMultipleDecks_HandlesCorrectly() throws Exception {
        String multiDeckJson = """
        {
            "data": [{
                "type": "seat-map",
                "number": "777",
                "carrierCode": "UA",
                "decks": [
                    {
                        "deckType": "MAIN",
                        "seats": [{
                            "number": "1A",
                            "characteristicsCodes": ["F"]
                        }]
                    },
                    {
                        "deckType": "UPPER",
                        "seats": [{
                            "number": "2A",
                            "characteristicsCodes": ["F"]
                        }]
                    }
                ]
            }]
        }
        """;
        
        JsonNode multiDeckResponse = objectMapper.readTree(multiDeckJson);
        SeatMapData result = amadeusService.convertToSeatMapData(multiDeckResponse);
        
        assertNotNull(result.getDecks());
        assertEquals(2, result.getDecks().size());
        assertEquals("MAIN", result.getDecks().get(0).getDeckType());
        assertEquals("UPPER", result.getDecks().get(1).getDeckType());
        assertEquals("1A", result.getDecks().get(0).getSeats().get(0).getNumber());
        assertEquals("2A", result.getDecks().get(1).getSeats().get(0).getNumber());
    }
    
    @Test
    void convertToSeatMapData_WithMalformedJson_HandlesGracefully() throws Exception {
        String malformedJson = """
        {
            "data": [{
                "number": 123,
                "departure": {
                    "at": "not-a-date"
                },
                "decks": [{
                    "seats": [{
                        "number": null
                    }]
                }]
            }]
        }
        """;
        
        JsonNode malformedResponse = objectMapper.readTree(malformedJson);
        
        // Should not throw exception, but handle gracefully
        assertDoesNotThrow(() -> {
            SeatMapData result = amadeusService.convertToSeatMapData(malformedResponse);
            assertNotNull(result);
            assertEquals("AMADEUS", result.getSource());
        });
    }
    
    @Test
    void convertToSeatMapData_WithMultiSegmentFlight_HandlesCorrectly() throws Exception {
        String multiSegmentJson = """
        {
            "data": [{
                "type": "seat-map",
                "number": "1",
                "carrierCode": "UA",
                "aircraft": {
                    "code": "73H"
                },
                "departure": {
                    "iataCode": "SFO",
                    "at": "2025-12-05T08:30:00"
                },
                "arrival": {
                    "iataCode": "DEN",
                    "at": "2025-12-05T11:45:00"
                },
                "decks": [{
                    "deckType": "MAIN",
                    "seats": [{
                        "number": "12A",
                        "characteristicsCodes": ["W"],
                        "travelerPricing": [{
                            "travelerId": "1",
                            "seatAvailabilityStatus": "AVAILABLE",
                            "price": {"currency": "USD", "total": "25.00"}
                        }]
                    }]
                }]
            }, {
                "type": "seat-map",
                "number": "456",
                "carrierCode": "UA",
                "aircraft": {
                    "code": "320"
                },
                "departure": {
                    "iataCode": "DEN",
                    "at": "2025-12-05T13:30:00"
                },
                "arrival": {
                    "iataCode": "CUN",
                    "at": "2025-12-05T18:45:00"
                },
                "decks": [{
                    "deckType": "MAIN",
                    "seats": [{
                        "number": "15F",
                        "characteristicsCodes": ["W"],
                        "travelerPricing": [{
                            "travelerId": "1",
                            "seatAvailabilityStatus": "AVAILABLE",
                            "price": {"currency": "USD", "total": "30.00"}
                        }]
                    }]
                }]
            }]
        }
        """;
        
        JsonNode multiSegmentResponse = objectMapper.readTree(multiSegmentJson);
        SeatMapData result = amadeusService.convertToSeatMapData(multiSegmentResponse);
        
        // Should process the first segment
        assertNotNull(result);
        assertEquals("AMADEUS", result.getSource());
        assertNotNull(result.getFlight());
        assertEquals("1", result.getFlight().getNumber());
        assertEquals("UA", result.getFlight().getCarrierCode());
        assertEquals("SFO", result.getFlight().getDeparture().getIataCode());
        assertEquals("DEN", result.getFlight().getArrival().getIataCode());
        
        // Verify aircraft and seat data from first segment
        assertNotNull(result.getAircraft());
        assertEquals("73H", result.getAircraft().getCode());
        
        assertNotNull(result.getDecks());
        assertEquals(1, result.getDecks().size());
        assertEquals("12A", result.getDecks().get(0).getSeats().get(0).getNumber());
    }
    
    @Test
    void convertToSeatMapData_WithSegmentMissingData_HandlesGracefully() throws Exception {
        String incompleteSegmentJson = """
        {
            "data": [{
                "type": "seat-map",
                "number": "1",
                "carrierCode": "UA",
                "departure": {
                    "iataCode": "SFO"
                },
                "arrival": {
                    "iataCode": "CUN"
                }
            }]
        }
        """;
        
        JsonNode incompleteResponse = objectMapper.readTree(incompleteSegmentJson);
        SeatMapData result = amadeusService.convertToSeatMapData(incompleteResponse);
        
        // Should handle missing data gracefully
        assertNotNull(result);
        assertEquals("AMADEUS", result.getSource());
        assertNotNull(result.getFlight());
        assertEquals("1", result.getFlight().getNumber());
        assertEquals("UA", result.getFlight().getCarrierCode());
        assertEquals("SFO", result.getFlight().getDeparture().getIataCode());
        assertEquals("CUN", result.getFlight().getArrival().getIataCode());
        
        // Missing data should be null but not cause errors
        assertNull(result.getAircraft());
        assertNull(result.getDecks());
    }
    
    @Test 
    void convertToSeatMapData_WithEmptySeatsArray_HandlesCorrectly() throws Exception {
        String emptySeatsJson = """
        {
            "data": [{
                "type": "seat-map",
                "number": "1",
                "carrierCode": "UA",
                "decks": [{
                    "deckType": "MAIN",
                    "seats": []
                }]
            }]
        }
        """;
        
        JsonNode emptySeatsResponse = objectMapper.readTree(emptySeatsJson);
        SeatMapData result = amadeusService.convertToSeatMapData(emptySeatsResponse);
        
        assertNotNull(result);
        assertEquals("AMADEUS", result.getSource());
        assertNotNull(result.getDecks());
        assertEquals(1, result.getDecks().size());
        
        SeatMapData.SeatMapDeck deck = result.getDecks().get(0);
        assertEquals("MAIN", deck.getDeckType());
        assertNotNull(deck.getSeats());
        assertEquals(0, deck.getSeats().size()); // Empty seats array
    }
    
    @Test
    void getBatchSeatMapsFromOffers_WithValidOffers_ReturnsJsonNode() throws Exception {
        // Arrange
        List<JsonNode> flightOffers = createMockFlightOffers();
        
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock batch seat map response
        String batchResponseJson = """
        {
            "data": [
                {
                    "type": "seat-map",
                    "flightOfferId": "offer1",
                    "number": "101",
                    "carrierCode": "AA",
                    "decks": [{
                        "deckType": "MAIN",
                        "seats": [{"number": "1A", "characteristicsCodes": ["W"]}]
                    }]
                },
                {
                    "type": "seat-map", 
                    "flightOfferId": "offer2",
                    "number": "201",
                    "carrierCode": "UA",
                    "decks": [{
                        "deckType": "MAIN",
                        "seats": [{"number": "12F", "characteristicsCodes": ["A"]}]
                    }]
                }
            ]
        }
        """;
        
        HttpResponse<String> batchResponse = mock(HttpResponse.class);
        when(batchResponse.statusCode()).thenReturn(200);
        when(batchResponse.body()).thenReturn(batchResponseJson);
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse)
            .thenReturn(batchResponse);
        
        // Act
        JsonNode result = amadeusService.getBatchSeatMapsFromOffers(flightOffers);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.has("data"));
        assertEquals(2, result.get("data").size());
    }
    
    @Test
    void searchFlightsWithBatchSeatmaps_WithValidParameters_ReturnsFilteredResults() throws Exception {
        // Arrange
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock flight offers response
        String flightOffersJson = """
        {
            "data": [
                {"id": "offer1", "type": "flight-offer"},
                {"id": "offer2", "type": "flight-offer"},
                {"id": "offer3", "type": "flight-offer"}
            ]
        }
        """;
        
        HttpResponse<String> flightOffersResponse = mock(HttpResponse.class);
        when(flightOffersResponse.statusCode()).thenReturn(200);
        when(flightOffersResponse.body()).thenReturn(flightOffersJson);
        
        // Mock individual seat map responses (only 2 out of 3 offers have seat maps)
        String seatMapJson1 = """
        {
            "data": [{
                "type": "seat-map",
                "number": "101",
                "carrierCode": "AA",
                "decks": [{"deckType": "MAIN", "seats": []}]
            }]
        }
        """;
        
        String seatMapJson2 = """
        {
            "data": [{
                "type": "seat-map",
                "number": "201", 
                "carrierCode": "UA",
                "decks": [{"deckType": "MAIN", "seats": []}]
            }]
        }
        """;
        
        HttpResponse<String> seatMapResponse1 = mock(HttpResponse.class);
        when(seatMapResponse1.statusCode()).thenReturn(200);
        when(seatMapResponse1.body()).thenReturn(seatMapJson1);
        
        HttpResponse<String> seatMapResponse2 = mock(HttpResponse.class);
        when(seatMapResponse2.statusCode()).thenReturn(200);
        when(seatMapResponse2.body()).thenReturn(seatMapJson2);
        
        HttpResponse<String> seatMapResponse3 = mock(HttpResponse.class);
        when(seatMapResponse3.statusCode()).thenReturn(400);
        when(seatMapResponse3.body()).thenReturn("{\"error\": \"Seat map not available\"}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse)
            .thenReturn(flightOffersResponse)
            .thenReturn(seatMapResponse1)  // First seat map call succeeds
            .thenReturn(seatMapResponse2)  // Second seat map call succeeds
            .thenReturn(seatMapResponse3); // Third seat map call fails
        
        // Act
        List<FlightSearchResult> results = amadeusService.searchFlightsWithBatchSeatmaps(
            "LAX", "JFK", "2024-12-15", "ECONOMY", null, 10
        );
        
        // Assert
        assertNotNull(results);
        assertEquals(2, results.size()); // Only 2 out of 3 offers had seat maps
        
        for (FlightSearchResult result : results) {
            assertEquals("AMADEUS", result.getDataSource());
            assertNotNull(result.getSeatMap());
            assertEquals("AMADEUS", result.getSeatMap().getSource());
        }
    }
    
    @Test 
    void getBatchSeatMapsFromOffers_WithNetworkError_ThrowsException() throws Exception {
        // Arrange
        List<JsonNode> flightOffers = createMockFlightOffers();
        
        // Mock token response first
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse)
            .thenThrow(new IOException("Network error"));
        
        // Act & Assert
        assertThrows(SeatmapApiException.class, () -> {
            amadeusService.getBatchSeatMapsFromOffers(flightOffers);
        });
    }
    
    @Test
    void getBatchSeatMapsFromOffers_WithApiError_ThrowsException() throws Exception {
        // Arrange
        List<JsonNode> flightOffers = createMockFlightOffers();
        
        // Mock token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"access_token\":\"test-token\",\"expires_in\":3600}");
        
        // Mock error response
        HttpResponse<String> errorResponse = mock(HttpResponse.class);
        when(errorResponse.statusCode()).thenReturn(400);
        when(errorResponse.body()).thenReturn("{\"error\": \"Invalid request\"}");
        
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
            .thenReturn(tokenResponse)
            .thenReturn(errorResponse);
        
        // Act & Assert
        assertThrows(SeatmapApiException.class, () -> {
            amadeusService.getBatchSeatMapsFromOffers(flightOffers);
        });
    }
    
    // Helper methods for batch tests
    
    private List<JsonNode> createMockFlightOffers() {
        List<JsonNode> offers = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        
        ObjectNode offer1 = mapper.createObjectNode();
        offer1.put("id", "offer1");
        offer1.put("type", "flight-offer");
        offers.add(offer1);
        
        ObjectNode offer2 = mapper.createObjectNode();
        offer2.put("id", "offer2");
        offer2.put("type", "flight-offer");
        offers.add(offer2);
        
        return offers;
    }
}