package com.seatmap.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SeatMapRequestTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void validRequest_PassesValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "2024-12-01", "LAX", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void emptyFlightNumber_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("", "2024-12-01", "LAX", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Flight number is required", violation.getMessage());
        assertEquals("flightNumber", violation.getPropertyPath().toString());
    }
    
    @Test
    void nullFlightNumber_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest(null, "2024-12-01", "LAX", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Flight number is required"));
    }
    
    @Test
    void emptyDepartureDate_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "", "LAX", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Departure date is required", violation.getMessage());
    }
    
    @Test
    void invalidDateFormat_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "12/01/2024", "LAX", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Departure date must be in YYYY-MM-DD format", violation.getMessage());
    }
    
    @Test
    void invalidDateFormat_WithInvalidCharacters_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "2024-AB-01", "LAX", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("YYYY-MM-DD format"));
    }
    
    @Test
    void emptyOrigin_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "2024-12-01", "", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Origin airport code is required", violation.getMessage());
    }
    
    @Test
    void invalidOriginLength_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "2024-12-01", "LAXX", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Origin must be a 3-letter IATA airport code", violation.getMessage());
    }
    
    @Test
    void invalidOriginWithNumbers_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "2024-12-01", "LA1", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("3-letter IATA airport code"));
    }
    
    @Test
    void lowercaseOrigin_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "2024-12-01", "lax", "JFK");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("3-letter IATA airport code"));
    }
    
    @Test
    void emptyDestination_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "2024-12-01", "LAX", "");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Destination airport code is required", violation.getMessage());
    }
    
    @Test
    void invalidDestinationLength_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AA123", "2024-12-01", "LAX", "JF");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Destination must be a 3-letter IATA airport code", violation.getMessage());
    }
    
    @Test
    void multipleValidationErrors_ReturnsAllErrors() {
        SeatMapRequest request = new SeatMapRequest("", "invalid-date", "LAXX", "");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(4, violations.size());
        
        // Verify all expected error messages are present
        Set<String> messages = violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(java.util.stream.Collectors.toSet());
        
        assertTrue(messages.contains("Flight number is required"));
        assertTrue(messages.contains("Departure date must be in YYYY-MM-DD format"));
        assertTrue(messages.contains("Origin must be a 3-letter IATA airport code"));
        assertTrue(messages.contains("Destination airport code is required"));
    }
    
    @Test
    void gettersAndSetters_WorkCorrectly() {
        SeatMapRequest request = new SeatMapRequest();
        
        request.setFlightNumber("DL456");
        request.setDepartureDate("2024-12-25");
        request.setOrigin("SFO");
        request.setDestination("BOS");
        
        assertEquals("DL456", request.getFlightNumber());
        assertEquals("2024-12-25", request.getDepartureDate());
        assertEquals("SFO", request.getOrigin());
        assertEquals("BOS", request.getDestination());
    }
    
    @Test
    void constructor_WithAllParameters_SetsFieldsCorrectly() {
        SeatMapRequest request = new SeatMapRequest("UA789", "2024-11-15", "ORD", "MIA");
        
        assertEquals("UA789", request.getFlightNumber());
        assertEquals("2024-11-15", request.getDepartureDate());
        assertEquals("ORD", request.getOrigin());
        assertEquals("MIA", request.getDestination());
    }
    
    @Test
    void defaultConstructor_CreatesEmptyObject() {
        SeatMapRequest request = new SeatMapRequest();
        
        assertNull(request.getFlightNumber());
        assertNull(request.getDepartureDate());
        assertNull(request.getOrigin());
        assertNull(request.getDestination());
    }
    
    @Test
    void validInternationalFlightNumbers_PassValidation() {
        // Test various international flight number formats
        String[] validFlightNumbers = {
            "LH401",    // Lufthansa
            "AF123",    // Air France  
            "BA456",    // British Airways
            "KL789",    // KLM
            "AA123",    // American Airlines
            "DL456",    // Delta
            "UA789",    // United
            "SW123"     // Southwest
        };
        
        for (String flightNumber : validFlightNumbers) {
            SeatMapRequest request = new SeatMapRequest(flightNumber, "2024-12-01", "LAX", "JFK");
            Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Flight number " + flightNumber + " should be valid");
        }
    }
    
    @Test
    void validInternationalAirportCodes_PassValidation() {
        // Test various international airport codes
        String[][] validAirportPairs = {
            {"LAX", "JFK"}, // US domestic
            {"LHR", "CDG"}, // London to Paris
            {"NRT", "ICN"}, // Tokyo to Seoul
            {"DXB", "DOH"}, // Dubai to Doha
            {"FRA", "AMS"}, // Frankfurt to Amsterdam
            {"SYD", "MEL"}  // Sydney to Melbourne
        };
        
        for (String[] airports : validAirportPairs) {
            SeatMapRequest request = new SeatMapRequest("AA123", "2024-12-01", airports[0], airports[1]);
            Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), 
                "Airport codes " + airports[0] + " to " + airports[1] + " should be valid");
        }
    }
}