package com.seatmap.api.model;

import jakarta.validation.constraints.NotBlank;

public class SeatMapRequest {
    @NotBlank(message = "Source is required")
    private String source;
    
    @NotBlank(message = "Flight offer data is required")
    private String flightOfferData; // JSON string of complete flight offer from search
    
    public SeatMapRequest() {}
    
    public SeatMapRequest(String source, String flightOfferData) {
        this.source = source;
        this.flightOfferData = flightOfferData;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getFlightOfferData() {
        return flightOfferData;
    }
    
    public void setFlightOfferData(String flightOfferData) {
        this.flightOfferData = flightOfferData;
    }
}