package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;

public class SeatMapResponse {
    private boolean success;
    private String message;
    private JsonNode data;
    private String flightNumber;
    private String departureDate;
    private String origin;
    private String destination;
    
    public SeatMapResponse() {}
    
    public SeatMapResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public static SeatMapResponse success(JsonNode data, String flightNumber, String departureDate, String origin, String destination) {
        SeatMapResponse response = new SeatMapResponse(true, "Seat map retrieved successfully");
        response.setData(data);
        response.setFlightNumber(flightNumber);
        response.setDepartureDate(departureDate);
        response.setOrigin(origin);
        response.setDestination(destination);
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