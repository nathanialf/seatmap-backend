package com.seatmap.api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SeatmapViewRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void defaultConstructor_CreatesEmptyRequest() {
        SeatmapViewRequest request = new SeatmapViewRequest();

        assertNull(request.getFlightId());
        assertNull(request.getDataSource());
    }

    @Test
    void constructorWithFlightId_SetsFlightId() {
        SeatmapViewRequest request = new SeatmapViewRequest("AA123-LAX-SFO-20250115");

        assertEquals("AA123-LAX-SFO-20250115", request.getFlightId());
        assertNull(request.getDataSource());
    }

    @Test
    void constructorWithFlightIdAndDataSource_SetsBothFields() {
        SeatmapViewRequest request = new SeatmapViewRequest("DL456-JFK-LAX-20250201", "AMADEUS");

        assertEquals("DL456-JFK-LAX-20250201", request.getFlightId());
        assertEquals("AMADEUS", request.getDataSource());
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        SeatmapViewRequest request = new SeatmapViewRequest();

        request.setFlightId("UA789-SFO-ORD-20250301");
        request.setDataSource("SABRE");

        assertEquals("UA789-SFO-ORD-20250301", request.getFlightId());
        assertEquals("SABRE", request.getDataSource());
    }

    @Test
    void validation_ValidRequest_PassesValidation() {
        SeatmapViewRequest request = new SeatmapViewRequest("AA123-LAX-SFO-20250115", "AMADEUS");

        Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_ValidRequestWithoutDataSource_PassesValidation() {
        // Data source is optional
        SeatmapViewRequest request = new SeatmapViewRequest("AA123-LAX-SFO-20250115", null);

        Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_BlankFlightId_FailsValidation() {
        SeatmapViewRequest request = new SeatmapViewRequest("", "AMADEUS");

        Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<SeatmapViewRequest> violation = violations.iterator().next();
        assertEquals("Flight ID is required", violation.getMessage());
        assertEquals("flightId", violation.getPropertyPath().toString());
    }

    @Test
    void validation_NullFlightId_FailsValidation() {
        SeatmapViewRequest request = new SeatmapViewRequest(null, "SABRE");

        Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<SeatmapViewRequest> violation = violations.iterator().next();
        assertEquals("Flight ID is required", violation.getMessage());
    }

    @Test
    void validation_WhitespaceOnlyFlightId_FailsValidation() {
        SeatmapViewRequest request = new SeatmapViewRequest("   ", "AMADEUS");

        Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<SeatmapViewRequest> violation = violations.iterator().next();
        assertEquals("Flight ID is required", violation.getMessage());
    }

    @Test
    void dataSourceField_CanBeNull() {
        SeatmapViewRequest request = new SeatmapViewRequest("AA123-LAX-SFO-20250115");
        request.setDataSource(null);

        assertNull(request.getDataSource());

        Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void dataSourceField_CanBeSet() {
        SeatmapViewRequest request = new SeatmapViewRequest("AA123-LAX-SFO-20250115");
        request.setDataSource("AMADEUS");

        assertEquals("AMADEUS", request.getDataSource());
    }

    @Test
    void dataSourceField_CanBeEmptyString() {
        SeatmapViewRequest request = new SeatmapViewRequest("AA123-LAX-SFO-20250115", "");

        assertEquals("", request.getDataSource());

        Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_AcceptsVariousFlightIdFormats() {
        String[] validFlightIds = {
            "AA123",
            "FLIGHT_12345",
            "UA789-SFO-LAX-20250115",
            "DL-456",
            "flight.id.with.dots",
            "123456789"
        };

        for (String flightId : validFlightIds) {
            SeatmapViewRequest request = new SeatmapViewRequest(flightId);
            Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Flight ID '" + flightId + "' should be valid");
        }
    }

    @Test
    void validation_AcceptsVariousDataSourceValues() {
        String[] validDataSources = {
            "AMADEUS",
            "SABRE",
            "amadeus",
            "sabre",
            "OTHER_SOURCE",
            "123",
            ""
        };

        for (String dataSource : validDataSources) {
            SeatmapViewRequest request = new SeatmapViewRequest("AA123", dataSource);
            Set<ConstraintViolation<SeatmapViewRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Data source '" + dataSource + "' should be valid");
        }
    }
}