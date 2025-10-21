package com.seatmap.common.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GuestAccessHistoryTest {
    
    private GuestAccessHistory guestAccess;
    private String testIp = "192.168.1.100";
    
    @BeforeEach
    void setUp() {
        guestAccess = new GuestAccessHistory(testIp);
    }
    
    @Test
    void constructor_WithIpAddress_InitializesCorrectly() {
        assertEquals(testIp, guestAccess.getIpAddress());
        assertEquals(0, guestAccess.getSeatmapRequestsUsed());
        assertNotNull(guestAccess.getFirstAccess());
        assertNull(guestAccess.getLastSeatmapRequest());
        assertNotNull(guestAccess.getExpiresAt());
        
        // Should expire 6 months from now
        Instant expectedExpiry = Instant.now().plusSeconds(6 * 30 * 24 * 60 * 60);
        long timeDiff = Math.abs(guestAccess.getExpiresAt().getEpochSecond() - expectedExpiry.getEpochSecond());
        assertTrue(timeDiff < 60, "Should expire approximately 6 months from now");
    }
    
    @Test
    void constructor_DefaultConstructor_InitializesCorrectly() {
        GuestAccessHistory defaultAccess = new GuestAccessHistory();
        
        assertNull(defaultAccess.getIpAddress());
        assertEquals(0, defaultAccess.getSeatmapRequestsUsed());
        assertNotNull(defaultAccess.getFirstAccess());
        assertNull(defaultAccess.getLastSeatmapRequest());
        assertNotNull(defaultAccess.getExpiresAt());
    }
    
    @Test
    void canMakeSeatmapRequest_NewUser_ReturnsTrue() {
        assertTrue(guestAccess.canMakeSeatmapRequest());
    }
    
    @Test
    void hasExceededSeatmapLimit_ZeroRequests_ReturnsFalse() {
        assertFalse(guestAccess.hasExceededSeatmapLimit());
    }
    
    @Test
    void hasExceededSeatmapLimit_OneRequest_ReturnsFalse() {
        guestAccess.setSeatmapRequestsUsed(1);
        assertFalse(guestAccess.hasExceededSeatmapLimit());
    }
    
    @Test
    void hasExceededSeatmapLimit_TwoRequests_ReturnsTrue() {
        guestAccess.setSeatmapRequestsUsed(2);
        assertTrue(guestAccess.hasExceededSeatmapLimit());
    }
    
    @Test
    void hasExceededSeatmapLimit_ThreeRequests_ReturnsTrue() {
        guestAccess.setSeatmapRequestsUsed(3);
        assertTrue(guestAccess.hasExceededSeatmapLimit());
    }
    
    @Test
    void getRemainingSeatmapRequests_NewUser_ReturnsTwo() {
        assertEquals(2, guestAccess.getRemainingSeatmapRequests());
    }
    
    @Test
    void getRemainingSeatmapRequests_OneUsed_ReturnsOne() {
        guestAccess.setSeatmapRequestsUsed(1);
        assertEquals(1, guestAccess.getRemainingSeatmapRequests());
    }
    
    @Test
    void getRemainingSeatmapRequests_TwoUsed_ReturnsZero() {
        guestAccess.setSeatmapRequestsUsed(2);
        assertEquals(0, guestAccess.getRemainingSeatmapRequests());
    }
    
    @Test
    void getRemainingSeatmapRequests_ThreeUsed_ReturnsZero() {
        guestAccess.setSeatmapRequestsUsed(3);
        assertEquals(0, guestAccess.getRemainingSeatmapRequests());
    }
    
    @Test
    void recordSeatmapRequest_IncrementsCounter() {
        assertEquals(0, guestAccess.getSeatmapRequestsUsed());
        assertNull(guestAccess.getLastSeatmapRequest());
        
        guestAccess.recordSeatmapRequest();
        
        assertEquals(1, guestAccess.getSeatmapRequestsUsed());
        assertNotNull(guestAccess.getLastSeatmapRequest());
    }
    
    @Test
    void recordSeatmapRequest_UpdatesExpirationTime() {
        Instant originalExpiry = guestAccess.getExpiresAt();
        
        // Wait a small amount to ensure different timestamps
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        guestAccess.recordSeatmapRequest();
        
        assertTrue(guestAccess.getExpiresAt().isAfter(originalExpiry));
    }
    
    @Test
    void recordSeatmapRequest_MultipleCalls_IncrementsCorrectly() {
        guestAccess.recordSeatmapRequest();
        assertEquals(1, guestAccess.getSeatmapRequestsUsed());
        
        guestAccess.recordSeatmapRequest();
        assertEquals(2, guestAccess.getSeatmapRequestsUsed());
        
        // Should now be at limit
        assertTrue(guestAccess.hasExceededSeatmapLimit());
        assertFalse(guestAccess.canMakeSeatmapRequest());
    }
    
    @Test
    void canMakeSeatmapRequest_AtLimit_ReturnsFalse() {
        guestAccess.setSeatmapRequestsUsed(2);
        assertFalse(guestAccess.canMakeSeatmapRequest());
    }
    
    @Test
    void canMakeSeatmapRequest_OverLimit_ReturnsFalse() {
        guestAccess.setSeatmapRequestsUsed(5);
        assertFalse(guestAccess.canMakeSeatmapRequest());
    }
    
    @Test
    void canMakeSeatmapRequest_UnderLimit_ReturnsTrue() {
        guestAccess.setSeatmapRequestsUsed(1);
        assertTrue(guestAccess.canMakeSeatmapRequest());
    }
    
    @Test
    void getSeatmapDenialMessage_ExceededLimit() {
        guestAccess.setSeatmapRequestsUsed(2);
        String message = guestAccess.getSeatmapDenialMessage();
        assertTrue(message.contains("You've used your 2 free seat map views"));
        assertTrue(message.contains("register"));
        assertTrue(message.contains("unlimited"));
    }
    
    @Test
    void getSeatmapDenialMessage_NotExceeded_ReturnsGenericMessage() {
        guestAccess.setSeatmapRequestsUsed(1);
        String message = guestAccess.getSeatmapDenialMessage();
        assertTrue(message.contains("temporarily unavailable"));
        assertTrue(message.contains("register"));
    }
    
    @Test
    void seatmapRequestFlow_FirstRequest() {
        // Initial state
        assertTrue(guestAccess.canMakeSeatmapRequest());
        assertEquals(2, guestAccess.getRemainingSeatmapRequests());
        
        // Make first request
        guestAccess.recordSeatmapRequest();
        
        // Should still be able to make one more
        assertTrue(guestAccess.canMakeSeatmapRequest());
        assertEquals(1, guestAccess.getRemainingSeatmapRequests());
        assertEquals(1, guestAccess.getSeatmapRequestsUsed());
    }
    
    @Test
    void seatmapRequestFlow_SecondRequest() {
        // Make first request
        guestAccess.recordSeatmapRequest();
        
        // Make second request
        guestAccess.recordSeatmapRequest();
        
        // Should now be at limit
        assertFalse(guestAccess.canMakeSeatmapRequest());
        assertEquals(0, guestAccess.getRemainingSeatmapRequests());
        assertEquals(2, guestAccess.getSeatmapRequestsUsed());
        assertTrue(guestAccess.hasExceededSeatmapLimit());
    }
    
    @Test
    void expirationTimeCalculation() {
        Instant creationTime = guestAccess.getExpiresAt();
        Instant expectedExpiry = Instant.now().plusSeconds(6 * 30 * 24 * 60 * 60); // 6 months
        
        // Should be within a few seconds of expected expiry (accounting for test execution time)
        long timeDifference = Math.abs(creationTime.getEpochSecond() - expectedExpiry.getEpochSecond());
        assertTrue(timeDifference < 60, "Expiration time should be approximately 6 months from now");
    }
    
    @Test
    void expirationTimeUpdate_OnSeatmapRequest() {
        Instant originalExpiry = guestAccess.getExpiresAt();
        
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        guestAccess.recordSeatmapRequest();
        
        // Expiration should be extended on activity
        assertTrue(guestAccess.getExpiresAt().isAfter(originalExpiry));
        
        // Should still be approximately 6 months from now
        Instant expectedExpiry = Instant.now().plusSeconds(6 * 30 * 24 * 60 * 60);
        long timeDiff = Math.abs(guestAccess.getExpiresAt().getEpochSecond() - expectedExpiry.getEpochSecond());
        assertTrue(timeDiff < 60, "Expiration should be reset to 6 months from now");
    }
}