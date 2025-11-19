package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlightSearchResultTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testConstructorRequiresDataSource() {
        // Create a flight offer without dataSource
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("type", "flight-offer");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        // Should throw IllegalArgumentException when dataSource is missing
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new FlightSearchResult(flightOffer, seatMapData, true, null)
        );
        
        assertTrue(exception.getMessage().contains("dataSource"));
        assertTrue(exception.getMessage().contains("provider routing"));
    }

    @Test
    void testConstructorRequiresNonEmptyDataSource() {
        // Create a flight offer with empty dataSource
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("type", "flight-offer");
        flightOffer.put("dataSource", ""); // Empty string
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        // Should throw IllegalArgumentException when dataSource is empty
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new FlightSearchResult(flightOffer, seatMapData, true, null)
        );
        
        assertTrue(exception.getMessage().contains("dataSource"));
    }

    @Test
    void testConstructorRequiresNonBlankDataSource() {
        // Create a flight offer with blank dataSource
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("type", "flight-offer");
        flightOffer.put("dataSource", "   "); // Whitespace only
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        // Should throw IllegalArgumentException when dataSource is blank
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new FlightSearchResult(flightOffer, seatMapData, true, null)
        );
        
        assertTrue(exception.getMessage().contains("dataSource"));
    }

    @Test
    void testConstructorSucceedsWithValidDataSource() {
        // Create a flight offer with valid dataSource
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("type", "flight-offer");
        flightOffer.put("dataSource", "AMADEUS");
        flightOffer.put("source", "GDS");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        // Should create successfully with valid dataSource
        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null);
        
        assertNotNull(result);
        assertEquals("AMADEUS", result.getDataSource());
        assertEquals("GDS", result.getSource());
        assertEquals("test-flight-id", result.getId());
        assertEquals("flight-offer", result.getType());
    }

    @Test
    void testDataSourceValidationAnnotations() {
        // Create FlightSearchResult with invalid dataSource using setters
        FlightSearchResult result = new FlightSearchResult();
        result.setDataSource(null);

        // Validate using Jakarta validation
        Set<ConstraintViolation<FlightSearchResult>> violations = validator.validate(result);
        
        // Should have validation errors for null dataSource
        assertFalse(violations.isEmpty());
        
        boolean hasNotNullViolation = violations.stream()
            .anyMatch(v -> v.getMessage().contains("dataSource is required"));
        assertTrue(hasNotNullViolation);
    }

    @Test
    void testDataSourceBlankValidation() {
        // Create FlightSearchResult with blank dataSource
        FlightSearchResult result = new FlightSearchResult();
        result.setDataSource("");

        // Validate using Jakarta validation
        Set<ConstraintViolation<FlightSearchResult>> violations = validator.validate(result);
        
        // Should have validation errors for blank dataSource
        assertFalse(violations.isEmpty());
        
        boolean hasNotBlankViolation = violations.stream()
            .anyMatch(v -> v.getMessage().contains("dataSource cannot be blank"));
        assertTrue(hasNotBlankViolation);
    }

    @Test
    void testAmadeusDataSourceIsSet() {
        // Test that AMADEUS dataSource is properly set
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "amadeus-flight-123");
        flightOffer.put("dataSource", "AMADEUS");
        flightOffer.put("source", "GDS");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null);
        
        assertEquals("AMADEUS", result.getDataSource());
        assertEquals("AMADEUS", result.getSeatMap().getSource());
    }

    @Test
    void testSabreDataSourceIsSet() {
        // Test that SABRE dataSource is properly set
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "sabre-flight-456");
        flightOffer.put("dataSource", "SABRE");
        flightOffer.put("source", "GDS");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("SABRE");

        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null);
        
        assertEquals("SABRE", result.getDataSource());
        assertEquals("SABRE", result.getSeatMap().getSource());
    }

    @Test
    void testConstructorWithIncludeRawFalse() {
        // Test that rawFlightOffer is not included when includeRaw is false
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("dataSource", "AMADEUS");
        flightOffer.put("source", "GDS");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null, false);
        
        assertNotNull(result);
        assertEquals("AMADEUS", result.getDataSource());
        assertNull(result.getRawFlightOffer());
    }

    @Test
    void testConstructorWithIncludeRawTrue() {
        // Test that rawFlightOffer is included when includeRaw is true
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("dataSource", "AMADEUS");
        flightOffer.put("source", "GDS");
        flightOffer.put("type", "flight-offer");
        flightOffer.put("price", objectMapper.createObjectNode().put("total", "299.00"));
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null, true);
        
        assertNotNull(result);
        assertEquals("AMADEUS", result.getDataSource());
        assertNotNull(result.getRawFlightOffer());
        
        // Verify rawFlightOffer contains the original data
        assertEquals("test-flight-id", result.getRawFlightOffer().path("id").asText());
        assertEquals("AMADEUS", result.getRawFlightOffer().path("dataSource").asText());
        assertEquals("flight-offer", result.getRawFlightOffer().path("type").asText());
    }

    @Test
    void testBackwardCompatibilityConstructor() {
        // Test that the original constructor still works and defaults to no raw data
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("dataSource", "AMADEUS");
        flightOffer.put("source", "GDS");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null);
        
        assertNotNull(result);
        assertEquals("AMADEUS", result.getDataSource());
        assertNull(result.getRawFlightOffer());
    }

    @Test
    void testRawFlightOfferInToJsonNode() {
        // Test that rawFlightOffer is included in toJsonNode() when present
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("dataSource", "AMADEUS");
        flightOffer.put("source", "GDS");
        flightOffer.put("type", "flight-offer");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null, true);
        JsonNode jsonResult = result.toJsonNode();
        
        assertTrue(jsonResult.has("rawFlightOffer"));
        assertEquals("test-flight-id", jsonResult.get("rawFlightOffer").path("id").asText());
    }

    @Test
    void testRawFlightOfferNotInToJsonNodeWhenNotIncluded() {
        // Test that rawFlightOffer is not included in toJsonNode() when not present
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("dataSource", "AMADEUS");
        flightOffer.put("source", "GDS");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null, false);
        JsonNode jsonResult = result.toJsonNode();
        
        assertFalse(jsonResult.has("rawFlightOffer"));
    }

    @Test
    void testRawFlightOfferGetterSetter() {
        // Test the getter and setter for rawFlightOffer
        FlightSearchResult result = new FlightSearchResult();
        
        assertNull(result.getRawFlightOffer());
        
        ObjectNode rawData = objectMapper.createObjectNode();
        rawData.put("testField", "testValue");
        
        result.setRawFlightOffer(rawData);
        
        assertNotNull(result.getRawFlightOffer());
        assertEquals("testValue", result.getRawFlightOffer().path("testField").asText());
    }

    @Test
    void testRawFlightOfferDeepCopy() {
        // Test that rawFlightOffer is a deep copy, not a reference
        ObjectNode flightOffer = objectMapper.createObjectNode();
        flightOffer.put("id", "test-flight-id");
        flightOffer.put("dataSource", "AMADEUS");
        flightOffer.put("modifiableField", "original");
        
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");

        FlightSearchResult result = new FlightSearchResult(flightOffer, seatMapData, true, null, true);
        
        // Modify the original flightOffer
        flightOffer.put("modifiableField", "modified");
        
        // The rawFlightOffer should still have the original value
        assertEquals("original", result.getRawFlightOffer().path("modifiableField").asText());
    }
}