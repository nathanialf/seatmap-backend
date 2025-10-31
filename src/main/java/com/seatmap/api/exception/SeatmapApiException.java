package com.seatmap.api.exception;

public class SeatmapApiException extends Exception {
    
    public SeatmapApiException(String message) {
        super(message);
    }
    
    public SeatmapApiException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SeatmapApiException(Throwable cause) {
        super(cause);
    }
}