package com.seatmap.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class FlightOffersRequest {
    @NotBlank(message = "Origin is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Origin must be a 3-letter airport code")
    private String origin;
    
    @NotBlank(message = "Destination is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Destination must be a 3-letter airport code")
    private String destination;
    
    @NotBlank(message = "Departure date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Departure date must be in YYYY-MM-DD format")
    private String departureDate;
    
    @Pattern(regexp = "^(ECONOMY|PREMIUM_ECONOMY|BUSINESS|FIRST)$", message = "Travel class must be ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST")
    private String travelClass; // Optional - minimum cabin quality, searches all classes if not specified
    
    private String flightNumber; // Optional for filtering specific flights
    private Integer maxResults = 10; // Optional, defaults to 10
    
    // Constructors
    public FlightOffersRequest() {}
    
    public FlightOffersRequest(String origin, String destination, String departureDate, String travelClass) {
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
}