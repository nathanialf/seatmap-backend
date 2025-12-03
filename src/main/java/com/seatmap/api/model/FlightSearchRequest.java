package com.seatmap.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class FlightSearchRequest {
    @NotNull(message = "Origin is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Origin must be a 3-letter airport code")
    private String origin;
    
    @NotNull(message = "Destination is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Destination must be a 3-letter airport code")
    private String destination;
    
    @NotNull(message = "Departure date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Departure date must be in YYYY-MM-DD format")
    private String departureDate;
    
    @Pattern(regexp = "^(ECONOMY|PREMIUM_ECONOMY|BUSINESS|FIRST)$", message = "Travel class must be ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST")
    private String travelClass; // Optional - minimum cabin quality, searches all classes if not specified
    
    @Pattern(regexp = "^[A-Z0-9]{2,3}$", message = "Airline code must be 2-3 uppercase alphanumeric characters")
    private String airlineCode; // Optional: "UA", "AA", etc.
    
    @Pattern(regexp = "^[0-9]{1,4}$", message = "Flight number must be 1-4 digits")
    private String flightNumber; // Optional: "1679", "123", etc. (requires airlineCode)
    
    @Min(value = 1, message = "Max results must be at least 1")
    @Max(value = 20, message = "Max results cannot exceed 20")
    private Integer maxResults = 10; // Optional, defaults to 10
    
    @Min(value = 0, message = "Offset must be 0 or greater")
    @Max(value = 100, message = "Offset cannot exceed 100 (5 pages × 20 max results)")
    private Integer offset = 0; // Optional, defaults to 0 for pagination
    
    private Boolean includeRawFlightOffer = false; // Optional, defaults to false for clean response
    
    // Constructors
    public FlightSearchRequest() {}
    
    public FlightSearchRequest(String origin, String destination, String departureDate, String travelClass) {
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
        this.travelClass = travelClass;
    }
    
    // Getters and setters
    public String getOrigin() {
        return origin;
    }
    
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public String getDepartureDate() {
        return departureDate;
    }
    
    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }
    
    public String getTravelClass() {
        return travelClass;
    }
    
    public void setTravelClass(String travelClass) {
        this.travelClass = travelClass;
    }
    
    public String getAirlineCode() {
        return airlineCode;
    }
    
    public void setAirlineCode(String airlineCode) {
        this.airlineCode = airlineCode;
    }
    
    public String getFlightNumber() {
        return flightNumber;
    }
    
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }
    
    public Integer getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
    
    public Integer getOffset() {
        return offset;
    }
    
    public void setOffset(Integer offset) {
        this.offset = offset;
    }
    
    public Boolean getIncludeRawFlightOffer() {
        return includeRawFlightOffer;
    }
    
    public void setIncludeRawFlightOffer(Boolean includeRawFlightOffer) {
        this.includeRawFlightOffer = includeRawFlightOffer;
    }
    
    @JsonIgnore
    public boolean isValid() {
        // If flightNumber is provided, airlineCode must also be provided
        if (flightNumber != null && !flightNumber.trim().isEmpty()) {
            return airlineCode != null && !airlineCode.trim().isEmpty();
        }
        return true;
    }
    
    @JsonIgnore
    public String getValidationError() {
        if (!isValid()) {
            return "Flight number can only be provided when airline code is also specified";
        }
        return null;
    }
    
    @JsonIgnore
    public String getCombinedFlightNumber() {
        if (airlineCode == null || airlineCode.trim().isEmpty()) {
            return null;
        }
        if (flightNumber == null || flightNumber.trim().isEmpty()) {
            return airlineCode.trim(); // Just "UA"
        }
        return airlineCode.trim() + flightNumber.trim(); // "UA1679"
    }
}