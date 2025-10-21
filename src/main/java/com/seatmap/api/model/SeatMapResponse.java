package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;

public class SeatMapResponse {
    private boolean success;
    private String message;
    private JsonNode data;
    private String flightOfferId;
    
    public SeatMapResponse() {}
    
    public SeatMapResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public static SeatMapResponse success(JsonNode data, String flightOfferId) {
        SeatMapResponse response = new SeatMapResponse(true, "Seat map retrieved successfully");
        response.setData(data);
        response.setFlightOfferId(flightOfferId);
        return response;
    }
    
    public static SeatMapResponse error(String message) {
        return new SeatMapResponse(false, message);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public JsonNode getData() {
        return data;
    }
    
    public void setData(JsonNode data) {
        this.data = data;
    }
    
    public String getFlightOfferId() {
        return flightOfferId;
    }
    
    public void setFlightOfferId(String flightOfferId) {
        this.flightOfferId = flightOfferId;
    }
}