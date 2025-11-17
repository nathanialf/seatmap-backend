package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        assertNull(response.getSource());
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
            "AMADEUS"
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertEquals(mockSeatMapData, response.getData());
        assertEquals("AMADEUS", response.getSource());
    }
    
    @Test
    void error_StaticMethod_CreatesErrorResponse() {
        SeatMapResponse response = SeatMapResponse.error("Error occurred");
        
        assertFalse(response.isSuccess());
        assertEquals("Error occurred", response.getMessage());
        assertNull(response.getData());
        assertNull(response.getSource());
    }
    
    @Test
    void settersAndGetters_WorkCorrectly() {
        SeatMapResponse response = new SeatMapResponse();
        
        response.setSuccess(true);
        response.setMessage("Test message");
        response.setData(mockSeatMapData);
        response.setSource("SABRE");
        
        assertTrue(response.isSuccess());
        assertEquals("Test message", response.getMessage());
        assertEquals(mockSeatMapData, response.getData());
        assertEquals("SABRE", response.getSource());
    }
    
    @Test
    void success_WithNullData_HandlesGracefully() {
        SeatMapResponse response = SeatMapResponse.success(
            null, 
            "AMADEUS"
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertNull(response.getData());
        assertEquals("AMADEUS", response.getSource());
    }
    
    @Test
    void success_WithEmptyFlightOfferId_HandlesGracefully() {
        SeatMapResponse response = SeatMapResponse.success(
            mockSeatMapData, 
            ""
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertEquals(mockSeatMapData, response.getData());
        assertEquals("", response.getSource());
    }
    
    @Test
    void success_WithNullFlightOfferId_HandlesGracefully() {
        SeatMapResponse response = SeatMapResponse.success(
            mockSeatMapData, 
            null
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertEquals(mockSeatMapData, response.getData());
        assertNull(response.getSource());
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
                        "flightOfferId": "AMADEUS",
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
            "AMADEUS"
        );
        
        assertTrue(response.isSuccess());
        assertEquals("Seat map retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertTrue(response.getData().has("data"));
        assertTrue(response.getData().get("data").isArray());
        assertEquals("AMADEUS", response.getSource());
    }
    
    @Test
    void response_HandlesSpecialCharactersInFlightOfferId() {
        SeatMapResponse response = SeatMapResponse.success(
            mockSeatMapData,
            "SABRE"
        );
        
        assertTrue(response.isSuccess());
        assertEquals("SABRE", response.getSource());
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
            "TEST123_OFFER"
        );
        
        JsonNode responseData = response.getData();
        assertEquals("preserved", responseData.get("data").get(0).get("nested").get("deeply").get("value").asText());
        assertEquals(3, responseData.get("data").get(0).get("array").size());
        assertTrue(responseData.get("data").get(0).get("boolean").asBoolean());
        assertTrue(responseData.get("data").get(0).get("null").isNull());
    }
    
    // Batch response tests
    
    @Test
    void batchSuccess_WithValidSeatMaps_ReturnsCorrectResponse() throws Exception {
        // Arrange
        List<SeatMapResponse.SeatMapResult> seatMaps = new ArrayList<>();
        
        JsonNode seatMapData1 = objectMapper.readTree("{\"number\": \"1A\", \"available\": true}");
        JsonNode seatMapData2 = objectMapper.readTree("{\"number\": \"12F\", \"available\": true}");
        
        seatMaps.add(new SeatMapResponse.SeatMapResult("offer1", seatMapData1));
        seatMaps.add(new SeatMapResponse.SeatMapResult("offer2", seatMapData2));
        
        // Act
        SeatMapResponse response = SeatMapResponse.batchSuccess(seatMaps, "AMADEUS", 3);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Batch seat maps retrieved successfully", response.getMessage());
        assertEquals("AMADEUS", response.getSource());
        assertEquals(3, response.getTotalRequested());
        assertEquals(2, response.getTotalReturned());
        assertEquals(2, response.getSeatMaps().size());
        
        // Check individual results
        SeatMapResponse.SeatMapResult result1 = response.getSeatMaps().get(0);
        assertEquals("offer1", result1.getFlightOfferId());
        assertTrue(result1.isAvailable());
        assertEquals("1A", result1.getSeatMapData().get("number").asText());
        
        SeatMapResponse.SeatMapResult result2 = response.getSeatMaps().get(1);
        assertEquals("offer2", result2.getFlightOfferId());
        assertTrue(result2.isAvailable());
        assertEquals("12F", result2.getSeatMapData().get("number").asText());
    }
    
    @Test
    void batchSuccess_WithMixedResults_HandlesCorrectly() throws Exception {
        // Arrange
        List<SeatMapResponse.SeatMapResult> seatMaps = new ArrayList<>();
        
        JsonNode seatMapData = objectMapper.readTree("{\"seats\": [\"1A\", \"1B\"]}");
        
        // One successful, one failed
        seatMaps.add(new SeatMapResponse.SeatMapResult("offer1", seatMapData));
        seatMaps.add(new SeatMapResponse.SeatMapResult("offer2", "Seat map not available"));
        
        // Act
        SeatMapResponse response = SeatMapResponse.batchSuccess(seatMaps, "AMADEUS", 2);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals(2, response.getTotalRequested());
        assertEquals(2, response.getTotalReturned()); // Both are returned, but one has error
        
        SeatMapResponse.SeatMapResult successResult = response.getSeatMaps().get(0);
        assertTrue(successResult.isAvailable());
        assertNotNull(successResult.getSeatMapData());
        
        SeatMapResponse.SeatMapResult failedResult = response.getSeatMaps().get(1);
        assertFalse(failedResult.isAvailable());
        assertEquals("Seat map not available", failedResult.getError());
        assertNull(failedResult.getSeatMapData());
    }
    
    @Test
    void batchSuccess_WithEmptyList_HandlesCorrectly() {
        // Arrange
        List<SeatMapResponse.SeatMapResult> emptySeatMaps = new ArrayList<>();
        
        // Act
        SeatMapResponse response = SeatMapResponse.batchSuccess(emptySeatMaps, "AMADEUS", 5);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Batch seat maps retrieved successfully", response.getMessage());
        assertEquals(5, response.getTotalRequested());
        assertEquals(0, response.getTotalReturned());
        assertTrue(response.getSeatMaps().isEmpty());
    }
    
    @Test
    void seatMapResult_SuccessConstructor_SetsCorrectValues() throws Exception {
        // Arrange
        JsonNode mockData = objectMapper.readTree("{\"seat\": \"1A\", \"available\": true}");
        
        // Act
        SeatMapResponse.SeatMapResult result = new SeatMapResponse.SeatMapResult("flight123", mockData);
        
        // Assert
        assertEquals("flight123", result.getFlightOfferId());
        assertTrue(result.isAvailable());
        assertNotNull(result.getSeatMapData());
        assertEquals("1A", result.getSeatMapData().get("seat").asText());
        assertNull(result.getError());
    }
    
    @Test
    void seatMapResult_ErrorConstructor_SetsCorrectValues() {
        // Act
        SeatMapResponse.SeatMapResult result = new SeatMapResponse.SeatMapResult("flight456", "API timeout error");
        
        // Assert
        assertEquals("flight456", result.getFlightOfferId());
        assertFalse(result.isAvailable());
        assertEquals("API timeout error", result.getError());
        assertNull(result.getSeatMapData());
    }
    
    @Test
    void seatMapResult_DefaultConstructor_CreatesEmptyObject() {
        // Act
        SeatMapResponse.SeatMapResult result = new SeatMapResponse.SeatMapResult();
        
        // Assert
        assertNull(result.getFlightOfferId());
        assertFalse(result.isAvailable()); // Default should be false
        assertNull(result.getSeatMapData());
        assertNull(result.getError());
    }
    
    @Test
    void seatMapResult_SettersAndGetters_WorkCorrectly() throws Exception {
        // Arrange
        SeatMapResponse.SeatMapResult result = new SeatMapResponse.SeatMapResult();
        JsonNode testData = objectMapper.readTree("{\"test\": \"value\"}");
        
        // Act
        result.setFlightOfferId("test-offer");
        result.setAvailable(true);
        result.setSeatMapData(testData);
        result.setError("test error");
        
        // Assert
        assertEquals("test-offer", result.getFlightOfferId());
        assertTrue(result.isAvailable());
        assertEquals("value", result.getSeatMapData().get("test").asText());
        assertEquals("test error", result.getError());
    }
    
    @Test
    void batchResponse_HandlesNullValues() {
        // Act
        SeatMapResponse response = SeatMapResponse.batchSuccess(null, null, 0);
        
        // Assert
        assertTrue(response.isSuccess());
        assertNull(response.getSeatMaps());
        assertNull(response.getSource());
        assertEquals(0, response.getTotalRequested());
        assertEquals(0, response.getTotalReturned()); // null list has size 0
    }
}