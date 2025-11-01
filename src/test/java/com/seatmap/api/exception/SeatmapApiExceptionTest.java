package com.seatmap.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeatmapApiExceptionTest {

    @Test
    void constructorWithMessage_ShouldSetMessage() {
        String message = "API error occurred";
        
        SeatmapApiException exception = new SeatmapApiException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructorWithMessageAndCause_ShouldSetBoth() {
        String message = "API error with cause";
        RuntimeException cause = new RuntimeException("Root cause");
        
        SeatmapApiException exception = new SeatmapApiException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void constructorWithCauseOnly_ShouldSetCause() {
        RuntimeException cause = new RuntimeException("Root cause error");
        
        SeatmapApiException exception = new SeatmapApiException(cause);
        
        assertEquals(cause, exception.getCause());
        assertEquals("java.lang.RuntimeException: Root cause error", exception.getMessage());
    }
}