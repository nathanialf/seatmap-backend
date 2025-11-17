package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class SeatMapRequest {
    @NotNull(message = "Flight offers data is required")
    @Size(min = 1, max = 50, message = "Between 1 and 50 flight offers are allowed per request")
    private List<JsonNode> data; // Array of flight offers from Amadeus search
    
    public SeatMapRequest() {}
    
    public SeatMapRequest(List<JsonNode> data) {
        this.data = data;
    }
    
    // Legacy constructor for backward compatibility - creates single element list
    @Deprecated
    public SeatMapRequest(String flightOfferData) {
        try {
            if (flightOfferData != null) {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                JsonNode flightOffer = objectMapper.readTree(flightOfferData);
                this.data = List.of(flightOffer);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid flight offer JSON data", e);
        }
    }
    
    public List<JsonNode> getData() {
        return data;
    }
    
    public void setData(List<JsonNode> data) {
        this.data = data;
    }
    
    // Legacy getter for backward compatibility - deprecated
    @Deprecated
    public String getFlightOfferData() {
        // Convert first flight offer to JSON string for backward compatibility
        if (data != null && !data.isEmpty()) {
            return data.get(0).toString();
        }
        return null;
    }
    
    // Legacy setter for backward compatibility - deprecated
    @Deprecated
    public void setFlightOfferData(String flightOfferData) {
        try {
            if (flightOfferData != null) {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                JsonNode flightOffer = objectMapper.readTree(flightOfferData);
                this.data = List.of(flightOffer);
            } else {
                this.data = null;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid flight offer JSON data", e);
        }
    }
}