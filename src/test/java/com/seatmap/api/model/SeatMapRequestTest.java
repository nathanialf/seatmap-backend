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
        SeatMapRequest request = new SeatMapRequest("offer123", "{\"id\":\"offer123\",\"source\":\"GDS\"}");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void emptyFlightOfferId_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("", "{\"id\":\"offer123\",\"source\":\"GDS\"}");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        ConstraintViolation<SeatMapRequest> violation = violations.iterator().next();
        assertEquals("Source is required", violation.getMessage());
        assertEquals("source", violation.getPropertyPath().toString());
    }
    
    @Test
    void nullSource_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest(null, "{\"id\":\"offer123\",\"source\":\"GDS\"}");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Source is required"));
    }
    
    @Test
    void emptyFlightOfferData_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AMADEUS", "");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Flight offer data is required"));
    }
    
    @Test
    void nullFlightOfferData_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("AMADEUS", null);
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Flight offer data is required"));
    }
    
    @Test
    void bothFieldsEmpty_FailsValidation() {
        SeatMapRequest request = new SeatMapRequest("", "");
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Source is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Flight offer data is required")));
    }
    
    @Test
    void gettersAndSetters_WorkCorrectly() {
        SeatMapRequest request = new SeatMapRequest();
        
        request.setSource("AMADEUS");
        request.setFlightOfferData("{\"id\":\"offer456\",\"source\":\"API\"}");
        
        assertEquals("AMADEUS", request.getSource());
        assertEquals("{\"id\":\"offer456\",\"source\":\"API\"}", request.getFlightOfferData());
    }
    
    @Test
    void constructor_WithAllParameters_SetsFieldsCorrectly() {
        SeatMapRequest request = new SeatMapRequest("SABRE", "{\"id\":\"offer789\",\"data\":{\"flights\":[]}}");
        
        assertEquals("SABRE", request.getSource());
        assertEquals("{\"id\":\"offer789\",\"data\":{\"flights\":[]}}", request.getFlightOfferData());
    }
    
    @Test
    void defaultConstructor_CreatesEmptyObject() {
        SeatMapRequest request = new SeatMapRequest();
        
        assertNull(request.getSource());
        assertNull(request.getFlightOfferData());
    }
    
    @Test
    void validComplexFlightOfferData_PassesValidation() {
        String complexOfferData = "{\"id\":\"offer789\",\"type\":\"flight-offer\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2024-12-07\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"at\":\"2024-12-08T08:00:00\"},\"arrival\":{\"iataCode\":\"SFO\",\"at\":\"2024-12-08T13:00:00\"},\"carrierCode\":\"AA\",\"number\":\"2211\",\"aircraft\":{\"code\":\"320\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"259.00\",\"base\":\"219.00\",\"fees\":[{\"amount\":\"0.00\",\"type\":\"SUPPLIER\"},{\"amount\":\"0.00\",\"type\":\"TICKETING\"}]},\"pricingOptions\":{\"fareType\":[\"PUBLISHED\"],\"includedCheckedBagsOnly\":true},\"validatingAirlineCodes\":[\"AA\"],\"travelerPricings\":[{\"travelerId\":\"1\",\"fareOption\":\"STANDARD\",\"travelerType\":\"ADULT\",\"price\":{\"currency\":\"USD\",\"total\":\"259.00\",\"base\":\"219.00\"},\"fareDetailsBySegment\":[{\"segmentId\":\"1\",\"cabin\":\"ECONOMY\",\"fareBasis\":\"KAA0AFDN\",\"class\":\"K\",\"includedCheckedBags\":{\"quantity\":0}}]}]}";
        SeatMapRequest request = new SeatMapRequest("offer789", complexOfferData);
        
        Set<ConstraintViolation<SeatMapRequest>> violations = validator.validate(request);
        
        assertTrue(violations.isEmpty());
    }
    
    
    
    
    
    
    
    
}