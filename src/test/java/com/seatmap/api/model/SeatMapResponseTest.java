package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeatMapResponseTest {
    
    private ObjectMapper objectMapper;
    private JsonNode mockSeatMapData;
    
    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        mockSeatMapData = objectMapper.readTree("""
            {
                "data": [
                    {
                        "type": "seat-map",
                        "flightNumber": "AA123",
                        "aircraft": {
                            "code": "32A"
                        },
                        "decks": [
                            {
                                "deckType": "MAIN",
                                "facilities": [],
                                "seats": [
                                    {
                                        "number": "1A",
                                        "characteristicsCodes": ["A", "W"],
                                        "travelerPricing": []
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
            """);
    }
    
    @Test
    void defaultConstructor_CreatesEmptyResponse() {
        SeatMapResponse response = new SeatMapResponse();
        
        assertFalse(response.isSuccess());
        assertNull(response.getMessage());
        assertNull(response.getData());
        assertNull(response.getFlightNumber());
        assertNull(response.getDepartureDate());
        assertNull(response.getOrigin());
        assertNull(response.getDestination());
    }
    
    @Test
    void constructorWithParameters_SetsSuccessAndMessage() {
        SeatMapResponse response = new SeatMapResponse(true, "Success message");
        
        assertTrue(response.isSuccess());
        assertEquals("Success message", response.getMessage());
        assertNull(response.getData());
    }
    
    @Test
    void success_StaticMethod_CreatesSuccessfulResponse() {
        SeatMapResponse response = SeatMapResponse.success(
            mockSeatMapData, 
            "AA123", 
            "2024-12-01", 
            "LAX", 
            "JFK"
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertEquals(mockSeatMapData, response.getData());
        assertEquals("AA123", response.getFlightNumber());
        assertEquals("2024-12-01", response.getDepartureDate());
        assertEquals("LAX", response.getOrigin());
        assertEquals("JFK", response.getDestination());
    }
    
    @Test
    void error_StaticMethod_CreatesErrorResponse() {
        SeatMapResponse response = SeatMapResponse.error("Error occurred");
        
        assertFalse(response.isSuccess());
        assertEquals("Error occurred", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getFlightNumber());
        assertNull(response.getDepartureDate());
        assertNull(response.getOrigin());
        assertNull(response.getDestination());
    }
    
    @Test
    void settersAndGetters_WorkCorrectly() {
        SeatMapResponse response = new SeatMapResponse();
        
        response.setSuccess(true);
        response.setMessage("Test message");
        response.setData(mockSeatMapData);
        response.setFlightNumber("DL456");
        response.setDepartureDate("2024-11-15");
        response.setOrigin("SFO");
        response.setDestination("BOS");
        
        assertTrue(response.isSuccess());
        assertEquals("Test message", response.getMessage());
        assertEquals(mockSeatMapData, response.getData());
        assertEquals("DL456", response.getFlightNumber());
        assertEquals("2024-11-15", response.getDepartureDate());
        assertEquals("SFO", response.getOrigin());
        assertEquals("BOS", response.getDestination());
    }
    
    @Test
    void success_WithNullData_HandlesGracefully() {
        SeatMapResponse response = SeatMapResponse.success(
            null, 
            "AA123", 
            "2024-12-01", 
            "LAX", 
            "JFK"
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertNull(response.getData());
        assertEquals("AA123", response.getFlightNumber());
    }
    
    @Test
    void success_WithEmptyStrings_HandlesGracefully() {
        SeatMapResponse response = SeatMapResponse.success(
            mockSeatMapData, 
            "", 
            "", 
            "", 
            ""
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertEquals(mockSeatMapData, response.getData());
        assertEquals("", response.getFlightNumber());
        assertEquals("", response.getDepartureDate());
        assertEquals("", response.getOrigin());
        assertEquals("", response.getDestination());
    }
    
    @Test
    void success_WithNullStrings_HandlesGracefully() {
        SeatMapResponse response = SeatMapResponse.success(
            mockSeatMapData, 
            null, 
            null, 
            null, 
            null
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertEquals(mockSeatMapData, response.getData());
        assertNull(response.getFlightNumber());
        assertNull(response.getDepartureDate());
        assertNull(response.getOrigin());
        assertNull(response.getDestination());
    }
    
    @Test
    void error_WithNullMessage_HandlesGracefully() {
        SeatMapResponse response = SeatMapResponse.error(null);
        
        assertFalse(response.isSuccess());
        assertNull(response.getMessage());
        assertNull(response.getData());
    }
    
    @Test
    void error_WithEmptyMessage_HandlesGracefully() {
        SeatMapResponse response = SeatMapResponse.error("");
        
        assertFalse(response.isSuccess());
        assertEquals("", response.getMessage());
        assertNull(response.getData());
    }
    
    @Test
    void success_WithComplexJsonData_HandlesCorrectly() throws Exception {
        JsonNode complexData = objectMapper.readTree("""
            {
                "data": [
                    {
                        "type": "seat-map",
                        "flightNumber": "LH401",
                        "departure": {
                            "iataCode": "FRA",
                            "terminal": "1"
                        },
                        "arrival": {
                            "iataCode": "LAX",
                            "terminal": "B"
                        },
                        "aircraft": {
                            "code": "333"
                        },
                        "decks": [
                            {
                                "deckType": "MAIN",
                                "deckConfiguration": {
                                    "width": 9,
                                    "length": 62
                                },
                                "facilities": [
                                    {
                                        "code": "LA",
                                        "column": "A",
                                        "row": "15"
                                    }
                                ],
                                "seats": [
                                    {
                                        "number": "1A",
                                        "characteristicsCodes": ["A", "W", "F"],
                                        "travelerPricing": [
                                            {
                                                "travelerId": "1",
                                                "seatAvailabilityStatus": "AVAILABLE",
                                                "price": {
                                                    "currency": "EUR",
                                                    "total": "75.00"
                                                }
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
            """);
        
        SeatMapResponse response = SeatMapResponse.success(
            complexData,
            "LH401",
            "2024-12-15",
            "FRA",
            "LAX"
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertTrue(response.getData().has("data"));
        assertTrue(response.getData().get("data").isArray());
        assertEquals("LH401", response.getFlightNumber());
    }
    
    @Test
    void response_HandlesSpecialCharactersInFlightInfo() {
        SeatMapResponse response = SeatMapResponse.success(
            mockSeatMapData,
            "ABC123", // ASCII characters in flight number
            "2024-12-01",
            "MUC", // Munich airport code
            "JFK"
        );
        
        assertTrue(response.isSuccess());
        assertEquals("ABC123", response.getFlightNumber());
        assertEquals("MUC", response.getOrigin());
    }
    
    @Test
    void response_HandlesLongMessages() {
        String longMessage = "This is a very long error message that might occur when there are multiple validation errors or detailed error descriptions from external APIs that need to be communicated back to the client application.";
        
        SeatMapResponse response = SeatMapResponse.error(longMessage);
        
        assertFalse(response.isSuccess());
        assertEquals(longMessage, response.getMessage());
    }
    
    @Test
    void success_PreservesJsonStructure() throws Exception {
        JsonNode originalData = objectMapper.readTree("""
            {
                "data": [
                    {
                        "nested": {
                            "deeply": {
                                "value": "preserved"
                            }
                        },
                        "array": [1, 2, 3],
                        "boolean": true,
                        "null": null
                    }
                ]
            }
            """);
        
        SeatMapResponse response = SeatMapResponse.success(
            originalData,
            "TEST123",
            "2024-12-01", 
            "TST",
            "TES"
        );
        
        JsonNode responseData = response.getData();
        assertEquals("preserved", responseData.get("data").get(0).get("nested").get("deeply").get("value").asText());
        assertEquals(3, responseData.get("data").get(0).get("array").size());
        assertTrue(responseData.get("data").get(0).get("boolean").asBoolean());
        assertTrue(responseData.get("data").get(0).get("null").isNull());
    }
}