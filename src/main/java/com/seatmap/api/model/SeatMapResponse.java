package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class SeatMapResponse {
    private boolean success;
    private String message;
    private JsonNode data; // For single seat map responses (legacy)
    private List<SeatMapResult> seatMaps; // For batch responses
    private String source;
    private int totalRequested;
    private int totalReturned;
    
    public SeatMapResponse() {}
    
    public SeatMapResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Legacy method for single seat map response
    public static SeatMapResponse success(JsonNode data, String source) {
        SeatMapResponse response = new SeatMapResponse(true, "Seat map retrieved successfully");
        response.setData(data);
        response.setSource(source);
        return response;
    }
    
    // New method for batch seat map response
    public static SeatMapResponse batchSuccess(List<SeatMapResult> seatMaps, String source, int totalRequested) {
        SeatMapResponse response = new SeatMapResponse(true, "Batch seat maps retrieved successfully");
        response.setSeatMaps(seatMaps);
        response.setSource(source);
        response.setTotalRequested(totalRequested);
        response.setTotalReturned(seatMaps != null ? seatMaps.size() : 0);
        return response;
    }
    
    public static SeatMapResponse error(String message) {
        return new SeatMapResponse(false, message);
    }
    
    // Inner class to represent individual seat map results in batch response
    public static class SeatMapResult {
        private String flightOfferId;
        private boolean available;
        private JsonNode seatMapData;
        private String error;
        
        public SeatMapResult() {}
        
        public SeatMapResult(String flightOfferId, JsonNode seatMapData) {
            this.flightOfferId = flightOfferId;
            this.seatMapData = seatMapData;
            this.available = true;
        }
        
        public SeatMapResult(String flightOfferId, String error) {
            this.flightOfferId = flightOfferId;
            this.error = error;
            this.available = false;
        }
        
        public String getFlightOfferId() {
            return flightOfferId;
        }
        
        public void setFlightOfferId(String flightOfferId) {
            this.flightOfferId = flightOfferId;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public void setAvailable(boolean available) {
            this.available = available;
        }
        
        public JsonNode getSeatMapData() {
            return seatMapData;
        }
        
        public void setSeatMapData(JsonNode seatMapData) {
            this.seatMapData = seatMapData;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
    }
    
    // Getters and setters
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
    
    public List<SeatMapResult> getSeatMaps() {
        return seatMaps;
    }
    
    public void setSeatMaps(List<SeatMapResult> seatMaps) {
        this.seatMaps = seatMaps;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public int getTotalRequested() {
        return totalRequested;
    }
    
    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }
    
    public int getTotalReturned() {
        return totalReturned;
    }
    
    public void setTotalReturned(int totalReturned) {
        this.totalReturned = totalReturned;
    }
}