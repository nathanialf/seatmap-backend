package com.seatmap.api.model;

import jakarta.validation.constraints.NotBlank;

public class SeatmapViewRequest {
    @NotBlank(message = "Flight ID is required")
    private String flightId;
    
    private String dataSource; // Optional: AMADEUS/SABRE
    
    // Default constructor
    public SeatmapViewRequest() {}
    
    // Constructor
    public SeatmapViewRequest(String flightId) {
        this.flightId = flightId;
    }
    
    // Constructor with data source
    public SeatmapViewRequest(String flightId, String dataSource) {
        this.flightId = flightId;
        this.dataSource = dataSource;
    }
    
    // Getters and setters
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }
    
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}