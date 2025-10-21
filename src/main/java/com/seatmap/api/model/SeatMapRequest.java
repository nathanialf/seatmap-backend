package com.seatmap.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SeatMapRequest {
    @NotBlank(message = "Flight number is required")
    private String flightNumber;
    
    @NotBlank(message = "Departure date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Departure date must be in YYYY-MM-DD format")
    private String departureDate;
    
    @NotBlank(message = "Origin airport code is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Origin must be a 3-letter IATA airport code")
    private String origin;
    
    @NotBlank(message = "Destination airport code is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Destination must be a 3-letter IATA airport code")
    private String destination;
    
    public SeatMapRequest() {}
    
    public SeatMapRequest(String flightNumber, String departureDate, String origin, String destination) {
        this.flightNumber = flightNumber;
        this.departureDate = departureDate;
        this.origin = origin;
        this.destination = destination;
    }
    
    public String getFlightNumber() {
        return flightNumber;
    }
    
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }
    
    public String getDepartureDate() {
        return departureDate;
    }
    
    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }
    
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
}