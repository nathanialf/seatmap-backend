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
        assertNull(request.getAirlineCode());
        assertNull(request.getFlightNumber());
        assertEquals(10, request.getMaxResults()); // Default value
        assertEquals(0, request.getOffset()); // Default value
        assertFalse(request.getIncludeRawFlightOffer()); // Default value
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
        request.setIncludeRawFlightOffer(true);

        assertEquals("JFK", request.getOrigin());
        assertEquals("LAX", request.getDestination());
        assertEquals("2025-02-01", request.getDepartureDate());
        assertEquals("BUSINESS", request.getTravelClass());
        request.setAirlineCode("UA");
        request.setFlightNumber("123");
        assertEquals("UA", request.getAirlineCode());
        assertEquals("123", request.getFlightNumber());
        assertEquals(20, request.getMaxResults());
        assertTrue(request.getIncludeRawFlightOffer());
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

        assertEquals(1, violations.size()); // Only @Pattern triggers for blank strings with @NotNull
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Origin must be a 3-letter airport code", violation.getMessage());
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

        assertEquals(1, violations.size()); // Only @Pattern triggers for blank strings with @NotNull
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Destination must be a 3-letter airport code", violation.getMessage());
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

        assertEquals(1, violations.size()); // Only @Pattern triggers for blank strings with @NotNull
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Departure date must be in YYYY-MM-DD format", violation.getMessage());
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

    @Test
    void includeRawFlightOffer_DefaultsToFalse() {
        FlightSearchRequest request = new FlightSearchRequest();
        
        assertFalse(request.getIncludeRawFlightOffer());
    }

    @Test
    void includeRawFlightOffer_CanBeSetToTrue() {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setIncludeRawFlightOffer(true);
        
        assertTrue(request.getIncludeRawFlightOffer());
    }

    @Test
    void includeRawFlightOffer_CanBeSetToFalse() {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setIncludeRawFlightOffer(false);
        
        assertFalse(request.getIncludeRawFlightOffer());
    }

    @Test
    void includeRawFlightOffer_CanBeSetToNull() {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setIncludeRawFlightOffer(null);
        
        assertNull(request.getIncludeRawFlightOffer());
    }

    @Test
    void includeRawFlightOffer_DoesNotAffectValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setIncludeRawFlightOffer(true);

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_ValidAirlineCode_PassesValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setAirlineCode("UA");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_InvalidAirlineCode_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setAirlineCode("INVALID");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Airline code must be 2-3 uppercase letters", violation.getMessage());
    }

    @Test
    void validation_ValidFlightNumber_PassesValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setAirlineCode("UA");
        request.setFlightNumber("123");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_InvalidFlightNumber_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setAirlineCode("UA");
        request.setFlightNumber("INVALID");

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Flight number must be 1-4 digits", violation.getMessage());
    }

    @Test
    void businessLogicValidation_FlightNumberWithoutAirlineCode_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setFlightNumber("123");
        // No airline code set

        assertFalse(request.isValid());
        assertEquals("Flight number can only be provided when airline code is also specified", request.getValidationError());
    }

    @Test
    void businessLogicValidation_AirlineCodeWithoutFlightNumber_PassesValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setAirlineCode("UA");
        // No flight number set

        assertTrue(request.isValid());
        assertNull(request.getValidationError());
    }

    @Test
    void businessLogicValidation_BothAirlineCodeAndFlightNumber_PassesValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setAirlineCode("UA");
        request.setFlightNumber("123");

        assertTrue(request.isValid());
        assertNull(request.getValidationError());
    }

    @Test
    void getCombinedFlightNumber_AirlineCodeOnly_ReturnsAirlineCode() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setAirlineCode("UA");

        assertEquals("UA", request.getCombinedFlightNumber());
    }

    @Test
    void getCombinedFlightNumber_BothFields_ReturnsCombined() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setAirlineCode("UA");
        request.setFlightNumber("123");

        assertEquals("UA123", request.getCombinedFlightNumber());
    }

    @Test
    void getCombinedFlightNumber_NoAirlineCode_ReturnsNull() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setFlightNumber("123");

        assertNull(request.getCombinedFlightNumber());
    }

    @Test
    void validation_MaxResultsOutOfRange_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setMaxResults(0); // Below minimum

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Max results must be at least 1", violation.getMessage());

        // Test maximum
        request.setMaxResults(100); // Above maximum
        violations = validator.validate(request);

        assertEquals(1, violations.size());
        violation = violations.iterator().next();
        assertEquals("Max results cannot exceed 20", violation.getMessage());
    }

    // Pagination Tests
    @Test
    void offset_DefaultsToZero() {
        FlightSearchRequest request = new FlightSearchRequest();
        
        assertEquals(0, request.getOffset());
    }

    @Test
    void offset_CanBeSetAndRetrieved() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setOffset(20);
        
        assertEquals(20, request.getOffset());
    }

    @Test
    void validation_ValidOffset_PassesValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setOffset(50);

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_OffsetAtMinimum_PassesValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setOffset(0);

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_OffsetAtMaximum_PassesValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setOffset(100);

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_OffsetBelowMinimum_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setOffset(-1);

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Offset must be 0 or greater", violation.getMessage());
    }

    @Test
    void validation_OffsetAboveMaximum_FailsValidation() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setOffset(101);

        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<FlightSearchRequest> violation = violations.iterator().next();
        assertEquals("Offset cannot exceed 100 (5 pages × 20 max results)", violation.getMessage());
    }

    @Test
    void validation_OffsetBoundaryValues_ValidateCorrectly() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        
        // Test common pagination values
        int[] validOffsets = {0, 10, 20, 40, 60, 80, 100};
        for (int offset : validOffsets) {
            request.setOffset(offset);
            Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Offset " + offset + " should be valid");
        }
    }

    @Test
    void offset_CanBeSetToNull() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setOffset(null);
        
        assertNull(request.getOffset());
    }

    @Test
    void pagination_CompleteParameterSet_WorksTogether() {
        FlightSearchRequest request = new FlightSearchRequest("SFO", "LAX", "2025-01-15", "ECONOMY");
        request.setMaxResults(15);
        request.setOffset(45); // Page 4 with 15 results per page
        
        assertEquals(15, request.getMaxResults());
        assertEquals(45, request.getOffset());
        
        Set<ConstraintViolation<FlightSearchRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }
}