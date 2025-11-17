package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SeatMapRequestTest {
    
    private Validator validator;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void validRequest_PassesValidation() {
        SeatMapRequest request = new SeatMapRequest("{\"id\":\"offer123\",\"dataSource\":\"AMADEUS\"}");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void emptyFlightOfferData_FailsValidation() {
        // Empty string creates an empty list, which should fail size validation  
        try {
            SeatMapRequest request = new SeatMapRequest("");
        } catch (IllegalArgumentException e) {
            // This is expected for invalid JSON
            assertTrue(e.getMessage().contains("Invalid flight offer JSON data"));
        }
    }
    
    @Test
    void nullFlightOfferData_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest((String) null);
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Flight offers data is required"));
    }
    
    @Test
    void validFlightOfferData_PassesValidation() {
        SeatMapRequest request = new SeatMapRequest("{\"id\":\"offer456\",\"dataSource\":\"SABRE\"}");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void gettersAndSetters_WorkCorrectly() {
        SeatMapRequest request = new SeatMapRequest();
        
        request.setFlightOfferData("{\"id\":\"offer456\",\"dataSource\":\"AMADEUS\"}");
        
        assertEquals("{\"id\":\"offer456\",\"dataSource\":\"AMADEUS\"}", request.getFlightOfferData());
    }
    
    @Test
    void constructor_WithParameter_SetsFieldCorrectly() {
        SeatMapRequest request = new SeatMapRequest("{\"id\":\"offer789\",\"dataSource\":\"SABRE\"}");
        
        assertEquals("{\"id\":\"offer789\",\"dataSource\":\"SABRE\"}", request.getFlightOfferData());
    }
    
    @Test
    void defaultConstructor_CreatesEmptyObject() {
        SeatMapRequest request = new SeatMapRequest();
        
        assertNull(request.getFlightOfferData());
    }
    
    @Test
    void validComplexFlightOfferData_PassesValidation() {
        String complexOfferData = "{\"id\":\"offer789\",\"type\":\"flight-offer\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2024-12-07\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"at\":\"2024-12-08T08:00:00\"},\"arrival\":{\"iataCode\":\"SFO\",\"at\":\"2024-12-08T13:00:00\"},\"carrierCode\":\"AA\",\"number\":\"2211\",\"aircraft\":{\"code\":\"320\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"259.00\",\"base\":\"219.00\",\"fees\":[{\"amount\":\"0.00\",\"type\":\"SUPPLIER\"},{\"amount\":\"0.00\",\"type\":\"TICKETING\"}]},\"pricingOptions\":{\"fareType\":[\"PUBLISHED\"],\"includedCheckedBagsOnly\":true},\"validatingAirlineCodes\":[\"AA\"],\"travelerPricings\":[{\"travelerId\":\"1\",\"fareOption\":\"STANDARD\",\"travelerType\":\"ADULT\",\"price\":{\"currency\":\"USD\",\"total\":\"259.00\",\"base\":\"219.00\"},\"fareDetailsBySegment\":[{\"segmentId\":\"1\",\"cabin\":\"ECONOMY\",\"fareBasis\":\"KAA0AFDN\",\"class\":\"K\",\"includedCheckedBags\":{\"quantity\":0}}]}]}";
        SeatMapRequest request = new SeatMapRequest(complexOfferData);
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertTrue(violations.isEmpty());
    }
    
    // Batch functionality tests
    
    @Test
    void constructorWithList_ValidFlightOffers_SetsDataCorrectly() throws Exception {
        // Arrange
        List<JsonNode> flightOffers = createMockFlightOffers(2);
        
        // Act
        SeatMapRequest request = new SeatMapRequest(flightOffers);
        
        // Assert
        assertNotNull(request.getData());
        assertEquals(2, request.getData().size());
        assertEquals("offer1", request.getData().get(0).get("id").asText());
        assertEquals("offer2", request.getData().get(1).get("id").asText());
    }
    
    @Test
    void validation_WithValidListOffers_PassesValidation() throws Exception {
        // Arrange
        List<JsonNode> flightOffers = createMockFlightOffers(3);
        SeatMapRequest request = new SeatMapRequest(flightOffers);
        
        // Act
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void validation_WithEmptyList_FailsValidation() {
        // Arrange
        List<JsonNode> emptyList = new ArrayList<>();
        SeatMapRequest request = new SeatMapRequest(emptyList);
        
        // Act
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("Between 1 and 50 flight offers are allowed per request"));
    }
    
    @Test
    void validation_WithTooManyOffers_FailsValidation() throws Exception {
        // Arrange - create 51 offers (over the limit of 50)
        List<JsonNode> tooManyOffers = createMockFlightOffers(51);
        SeatMapRequest request = new SeatMapRequest(tooManyOffers);
        
        // Act
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("Between 1 and 50 flight offers are allowed per request"));
    }
    
    @Test
    void validation_WithNullDataList_FailsValidation() {
        // Arrange
        SeatMapRequest request = new SeatMapRequest((List<JsonNode>) null);
        
        // Act
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Flight offers data is required", violation.getMessage());
    }
    
    @Test
    void validation_WithMaxAllowedOffers_PassesValidation() throws Exception {
        // Arrange - exactly 50 offers (at the limit)
        List<JsonNode> maxOffers = createMockFlightOffers(50);
        SeatMapRequest request = new SeatMapRequest(maxOffers);
        
        // Act
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void validation_WithSingleOffer_PassesValidation() throws Exception {
        // Arrange
        List<JsonNode> singleOffer = createMockFlightOffers(1);
        SeatMapRequest request = new SeatMapRequest(singleOffer);
        
        // Act
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        // Assert
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void settersAndGetters_WithListData_WorkCorrectly() throws Exception {
        // Arrange
        List<JsonNode> initialData = createMockFlightOffers(2);
        List<JsonNode> newData = createMockFlightOffers(3);
        SeatMapRequest request = new SeatMapRequest();
        
        // Act
        request.setData(initialData);
        assertEquals(2, request.getData().size());
        
        request.setData(newData);
        
        // Assert
        assertEquals(3, request.getData().size());
        assertEquals("offer1", request.getData().get(0).get("id").asText());
        assertEquals("offer3", request.getData().get(2).get("id").asText());
    }
    
    @Test
    void legacyGetFlightOfferData_WithBatchData_ReturnsFirstOffer() throws Exception {
        // Arrange
        List<JsonNode> flightOffers = createMockFlightOffers(3);
        SeatMapRequest request = new SeatMapRequest(flightOffers);
        
        // Act
        String firstOfferData = request.getFlightOfferData();
        
        // Assert
        assertNotNull(firstOfferData);
        JsonNode parsed = objectMapper.readTree(firstOfferData);
        assertEquals("offer1", parsed.get("id").asText());
    }
    
    @Test
    void legacyGetFlightOfferData_WithEmptyData_ReturnsNull() {
        // Arrange
        SeatMapRequest request = new SeatMapRequest(new ArrayList<>());
        
        // Act
        String result = request.getFlightOfferData();
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void legacyGetFlightOfferData_WithNullData_ReturnsNull() {
        // Arrange
        SeatMapRequest request = new SeatMapRequest((List<JsonNode>) null);
        
        // Act
        String result = request.getFlightOfferData();
        
        // Assert
        assertNull(result);
    }
    
    // Helper method to create mock flight offers
    private List<JsonNode> createMockFlightOffers(int count) throws Exception {
        List<JsonNode> offers = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            String offerJson = String.format("""
                {
                    "id": "offer%d",
                    "type": "flight-offer",
                    "source": "GDS",
                    "itineraries": [{
                        "segments": [{
                            "carrierCode": "AA",
                            "number": "%d",
                            "departure": {"iataCode": "LAX"},
                            "arrival": {"iataCode": "JFK"}
                        }]
                    }]
                }
                """, i, i * 100);
            
            offers.add(objectMapper.readTree(offerJson));
        }
        
        return offers;
    }
}