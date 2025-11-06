package com.seatmap.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlightSearchRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void defaultConstructor_CreatesEmptyRequest() {
        FlightSearchRequest request = new FlightSearchRequest();

        assertNull(request.getOrigin());
        assertNull(request.getDestination());
        assertNull(request.getDepartureDate());
        assertNull(request.getTravelClass());
        assertNull(request.getFlightNumber());
        assertEquals(10, request.getMaxResults()); // Default value
    }

    @Test
    void constructorWithParameters_SetsFields() {
        FlightSearchRequest request = new FlightSearchRequest("LAX", "SFO", "2025-01-15", "ECONOMY");

        assertEquals("LAX", request.getOrigin());
        assertEquals("SFO", request.getDestination());
        assertEquals("2025-01-15", request.getDepartureDate());
        assertEquals("ECONOMY", request.getTravelClass());
        assertNull(request.getFlightNumber()); // Not set by constructor
        assertEquals(10, request.getMaxResults()); // Default value
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        FlightSearchRequest request = new FlightSearchRequest();

        request.setOrigin("JFK");
        request.setDestination("LAX");
        request.setDepartureDate("2025-02-01");
        request.setTravelClass("BUSINESS");
        request.setFlightNumber("UA123");
        request.setMaxResults(20);

        assertEquals("JFK", request.getOrigin());
        assertEquals("LAX", request.getDestination());
        assertEquals("2025-02-01", request.getDepartureDate());
        assertEquals("BUSINESS", request.getTravelClass());
        assertEquals("UA123", request.getFlightNumber());
        assertEquals(20, request.getMaxResults());
    }

    @Test
    void validation_ValidRequest_PassesValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_BlankOrigin_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("", "LAX", "2025-01-15", "ECONOMY");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(2, violations.size()); // Both @NotBlank and @Pattern trigger for blank strings
        boolean hasNotBlankViolation = violations.stream()
            .anyMatch(v -> "Origin is required".equals(v.getMessage()));
        boolean hasPatternViolation = violations.stream()
            .anyMatch(v -> "Origin must be a 3-letter airport code".equals(v.getMessage()));
        
        assertTrue(hasNotBlankViolation, "Should have NotBlank violation");
        assertTrue(hasPatternViolation, "Should have Pattern violation");
    }

    @Test
    void validation_NullOrigin_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest(null, "LAX", "2025-01-15", "ECONOMY");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Origin is required", violation.getMessage());
    }

    @Test
    void validation_InvalidOriginFormat_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("INVALID", "LAX", "2025-01-15", "ECONOMY");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Origin must be a 3-letter airport code", violation.getMessage());
    }

    @Test
    void validation_BlankDestination_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "", "2025-01-15", "ECONOMY");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(2, violations.size()); // Both @NotBlank and @Pattern trigger for blank strings
        boolean hasNotBlankViolation = violations.stream()
            .anyMatch(v -> "Destination is required".equals(v.getMessage()));
        boolean hasPatternViolation = violations.stream()
            .anyMatch(v -> "Destination must be a 3-letter airport code".equals(v.getMessage()));
        
        assertTrue(hasNotBlankViolation, "Should have NotBlank violation");
        assertTrue(hasPatternViolation, "Should have Pattern violation");
    }

    @Test
    void validation_InvalidDestinationFormat_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "XX", "2025-01-15", "ECONOMY");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Destination must be a 3-letter airport code", violation.getMessage());
    }

    @Test
    void validation_BlankDepartureDate_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "", "ECONOMY");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(2, violations.size()); // Both @NotBlank and @Pattern trigger for blank strings
        boolean hasNotBlankViolation = violations.stream()
            .anyMatch(v -> "Departure date is required".equals(v.getMessage()));
        boolean hasPatternViolation = violations.stream()
            .anyMatch(v -> "Departure date must be in YYYY-MM-DD format".equals(v.getMessage()));
        
        assertTrue(hasNotBlankViolation, "Should have NotBlank violation");
        assertTrue(hasPatternViolation, "Should have Pattern violation");
    }

    @Test
    void validation_InvalidDateFormat_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025/01/15", "ECONOMY");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Departure date must be in YYYY-MM-DD format", violation.getMessage());
    }

    @Test
    void validation_ValidTravelClasses_PassValidation() {
        String[] validClasses = {"ECONOMY", "PREMIUM_ECONOMY", "BUSINESS", "FIRST"};
        
        for (String travelClass : validClasses) {
            FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", travelClass);
            Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Travel class " + travelClass + " should be valid");
        }
    }

    @Test
    void validation_InvalidTravelClass_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "INVALID_CLASS");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Travel class must be ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST", violation.getMessage());
    }

    @Test
    void validation_NullTravelClass_PassesValidation() {
        // Travel class is optional
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", null);

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_EmptyTravelClass_FailsValidation() {
        // Empty travel class should fail the pattern validation
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Travel class must be ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST", violation.getMessage());
    }

    @Test
    void optionalFields_CanBeNull() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", null);
        request.setFlightNumber(null);
        request.setMaxResults(null);

        assertNull(request.getTravelClass());
        assertNull(request.getFlightNumber());
        assertNull(request.getMaxResults());

        // Should still pass validation for required fields
        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void optionalFields_CanBeSet() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", null);
        request.setFlightNumber("DL123");
        request.setMaxResults(25);

        assertEquals("DL123", request.getFlightNumber());
        assertEquals(25, request.getMaxResults());
    }

    @Test
    void validation_MultipleErrors_ReportsAllViolations() {
        FlightSearchRequest request = new FlightSearchRequest("", "", "", "INVALID");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        // Blank fields trigger both @NotBlank and @Pattern validations, so expect more violations
        assertTrue(violations.size() >= 4); // At least origin blank, destination blank, date blank, invalid travel class
    }
}