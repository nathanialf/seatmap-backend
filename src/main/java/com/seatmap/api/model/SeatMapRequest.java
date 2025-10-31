package com.seatmap.api.model;

import jakarta.validation.constraints.NotBlank;

public class SeatMapRequest {
    @NotBlank(message = "Flight offer data is required")
    private String flightOfferData; // JSON string of complete flight offer from search
    
    public SeatMapRequest() {}
    
    public SeatMapRequest(String flightOfferData) {
        this.flightOfferData = flightOfferData;
    }
    
    public String getFlightOfferData() {
        return flightOfferData;
    }
    
    public void setFlightOfferData(String flightOfferData) {
        this.flightOfferData = flightOfferData;
    }
}