package com.seatmap.api.model;

import jakarta.validation.constraints.NotBlank;

public class SeatMapRequest {
    @NotBlank(message = "Flight offer ID is required")
    private String flightOfferId;
    
    @NotBlank(message = "Flight offer data is required")
    private String flightOfferData; // JSON string of complete flight offer from search
    
    public SeatMapRequest() {}
    
    public SeatMapRequest(String flightOfferId, String flightOfferData) {
        this.flightOfferId = flightOfferId;
        this.flightOfferData = flightOfferData;
    }
    
    public String getFlightOfferId() {
        return flightOfferId;
    }
    
    public void setFlightOfferId(String flightOfferId) {
        this.flightOfferId = flightOfferId;
    }
    
    public String getFlightOfferData() {
        return flightOfferData;
    }
    
    public void setFlightOfferData(String flightOfferData) {
        this.flightOfferData = flightOfferData;
    }
}