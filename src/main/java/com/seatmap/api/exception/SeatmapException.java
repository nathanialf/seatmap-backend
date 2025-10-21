package com.seatmap.api.exception;

public class SeatmapException extends Exception {
    
    public SeatmapException(String message) {
        super(message);
    }
    
    public SeatmapException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public SeatmapException(Throwable cause) {
        super(cause);
    }
}