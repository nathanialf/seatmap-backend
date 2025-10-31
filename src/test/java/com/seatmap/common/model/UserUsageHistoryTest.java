package com.seatmap.common.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class UserUsageHistoryTest {
    
    private UserUsageHistory userUsageHistory;
    private final String testUserId = "test-user-123";
    
    @BeforeEach
    void setUp() {
        userUsageHistory = new UserUsageHistory();
    }
    
    @Test
    void constructor_DefaultValues_ShouldBeSetCorrectly() {
        assertEquals(Integer.valueOf(0), userUsageHistory.getBookmarksCreated());
        assertEquals(Integer.valueOf(0), userUsageHistory.getSeatmapRequestsUsed());
        assertNotNull(userUsageHistory.getCreatedAt());
        assertNotNull(userUsageHistory.getUpdatedAt());
        assertNotNull(userUsageHistory.getExpiresAt());
        
        // TTL should be 13 months from now
        Instant expectedExpiry = Instant.now().plusSeconds(13 * 30 * 24 * 60 * 60);
        assertTrue(Math.abs(userUsageHistory.getExpiresAt().getEpochSecond() - expectedExpiry.getEpochSecond()) < 5);
    }
    
    @Test
    void constructorWithUserId_ShouldSetUserIdAndCurrentMonth() {
        UserUsageHistory history = new UserUsageHistory(testUserId);
        
        assertEquals(testUserId, history.getUserId());
        assertEquals(UserUsageHistory.getCurrentMonthYear(), history.getMonthYear());
    }
    
    @Test
    void constructorWithUserIdAndMonth_ShouldSetBothValues() {
        String monthYear = "2024-03";
        UserUsageHistory history = new UserUsageHistory(testUserId, monthYear);
        
        assertEquals(testUserId, history.getUserId());
        assertEquals(monthYear, history.getMonthYear());
    }
    
    @Test
    void getCurrentMonthYear_ShouldReturnCorrectFormat() {
        String currentMonth = UserUsageHistory.getCurrentMonthYear();
        String expectedFormat = YearMonth.now(ZoneOffset.UTC).toString();
        
        assertEquals(expectedFormat, currentMonth);
        assertTrue(currentMonth.matches("\\d{4}-\\d{2}"));
    }
    
    @Test
    void recordBookmarkCreation_ShouldIncrementCountAndUpdateTimestamp() {
        Instant originalUpdate = userUsageHistory.getUpdatedAt();
        
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        userUsageHistory.recordBookmarkCreation();
        
        assertEquals(Integer.valueOf(1), userUsageHistory.getBookmarksCreated());
        assertNotNull(userUsageHistory.getLastBookmarkCreated());
        assertTrue(userUsageHistory.getUpdatedAt().isAfter(originalUpdate));
    }
    
    @Test
    void recordSeatmapRequest_ShouldIncrementCountAndUpdateTimestamp() {
        Instant originalUpdate = userUsageHistory.getUpdatedAt();
        
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        userUsageHistory.recordSeatmapRequest();
        
        assertEquals(Integer.valueOf(1), userUsageHistory.getSeatmapRequestsUsed());
        assertNotNull(userUsageHistory.getLastSeatmapRequest());
        assertTrue(userUsageHistory.getUpdatedAt().isAfter(originalUpdate));
    }
    
    @Test
    void hasExceededBookmarkLimit_WithinLimit_ShouldReturnFalse() {
        userUsageHistory.setBookmarksCreated(5);
        
        assertFalse(userUsageHistory.hasExceededBookmarkLimit(10));
        assertFalse(userUsageHistory.hasExceededBookmarkLimit(-1)); // Unlimited
    }
    
    @Test
    void hasExceededBookmarkLimit_AtLimit_ShouldReturnTrue() {
        userUsageHistory.setBookmarksCreated(10);
        
        assertTrue(userUsageHistory.hasExceededBookmarkLimit(10));
    }
    
    @Test
    void hasExceededBookmarkLimit_ExceedsLimit_ShouldReturnTrue() {
        userUsageHistory.setBookmarksCreated(15);
        
        assertTrue(userUsageHistory.hasExceededBookmarkLimit(10));
    }
    
    @Test
    void hasExceededBookmarkLimit_UnlimitedTier_ShouldReturnFalse() {
        userUsageHistory.setBookmarksCreated(1000);
        
        assertFalse(userUsageHistory.hasExceededBookmarkLimit(-1));
    }
    
    @Test
    void hasExceededSeatmapLimit_WithinLimit_ShouldReturnFalse() {
        userUsageHistory.setSeatmapRequestsUsed(50);
        
        assertFalse(userUsageHistory.hasExceededSeatmapLimit(100));
        assertFalse(userUsageHistory.hasExceededSeatmapLimit(-1)); // Unlimited
    }
    
    @Test
    void hasExceededSeatmapLimit_AtLimit_ShouldReturnTrue() {
        userUsageHistory.setSeatmapRequestsUsed(100);
        
        assertTrue(userUsageHistory.hasExceededSeatmapLimit(100));
    }
    
    @Test
    void getRemainingBookmarks_WithinLimit_ShouldReturnCorrectCount() {
        userUsageHistory.setBookmarksCreated(3);
        
        assertEquals(7, userUsageHistory.getRemainingBookmarks(10));
        assertEquals(Integer.MAX_VALUE, userUsageHistory.getRemainingBookmarks(-1));
    }
    
    @Test
    void getRemainingBookmarks_ExceedsLimit_ShouldReturnZero() {
        userUsageHistory.setBookmarksCreated(15);
        
        assertEquals(0, userUsageHistory.getRemainingBookmarks(10));
    }
    
    @Test
    void getRemainingBookmarks_AtLimit_ShouldReturnZero() {
        userUsageHistory.setBookmarksCreated(10);
        
        assertEquals(0, userUsageHistory.getRemainingBookmarks(10));
    }
    
    @Test
    void getRemainingBookmarks_UnlimitedTier_ShouldReturnMaxValue() {
        userUsageHistory.setBookmarksCreated(1000);
        
        assertEquals(Integer.MAX_VALUE, userUsageHistory.getRemainingBookmarks(-1));
    }
    
    @Test
    void getRemainingBookmarks_ZeroLimit_ShouldHandleCorrectly() {
        userUsageHistory.setBookmarksCreated(0);
        
        assertEquals(0, userUsageHistory.getRemainingBookmarks(0));
    }
    
    @Test
    void getRemainingBookmarks_NegativeUsage_ShouldHandleCorrectly() {
        userUsageHistory.setBookmarksCreated(-1); // Edge case - shouldn't happen in real usage
        
        assertEquals(11, userUsageHistory.getRemainingBookmarks(10));
    }
    
    @Test
    void canCreateBookmark_WithinLimit_ShouldReturnTrue() {
        userUsageHistory.setBookmarksCreated(5);
        
        assertTrue(userUsageHistory.canCreateBookmark(10));
        assertTrue(userUsageHistory.canCreateBookmark(-1));
    }
    
    @Test
    void canCreateBookmark_AtLimit_ShouldReturnFalse() {
        userUsageHistory.setBookmarksCreated(10);
        
        assertFalse(userUsageHistory.canCreateBookmark(10));
    }
    
    @Test
    void canCreateBookmark_ZeroLimit_ShouldReturnFalse() {
        userUsageHistory.setBookmarksCreated(0);
        
        assertFalse(userUsageHistory.canCreateBookmark(0));
    }
    
    @Test
    void canMakeSeatmapRequest_WithinLimit_ShouldReturnTrue() {
        userUsageHistory.setSeatmapRequestsUsed(50);
        
        assertTrue(userUsageHistory.canMakeSeatmapRequest(100));
        assertTrue(userUsageHistory.canMakeSeatmapRequest(-1));
    }
    
    @Test
    void canMakeSeatmapRequest_AtLimit_ShouldReturnFalse() {
        userUsageHistory.setSeatmapRequestsUsed(100);
        
        assertFalse(userUsageHistory.canMakeSeatmapRequest(100));
    }
    
    @Test
    void getBookmarkLimitMessage_ZeroLimit_ShouldReturnUpgradeMessage() {
        String message = userUsageHistory.getBookmarkLimitMessage(0, "FREE");
        
        assertTrue(message.contains("Bookmark creation is not available for FREE tier"));
        assertTrue(message.contains("upgrade to PRO or BUSINESS"));
    }
    
    @Test
    void getBookmarkLimitMessage_RegularLimit_ShouldReturnLimitMessage() {
        String message = userUsageHistory.getBookmarkLimitMessage(50, "PRO");
        
        assertTrue(message.contains("monthly limit of 50 bookmarks"));
        assertTrue(message.contains("PRO tier"));
        assertTrue(message.contains("Upgrade to BUSINESS"));
    }
    
    @Test
    void getSeatmapLimitMessage_ShouldReturnCorrectMessage() {
        String message = userUsageHistory.getSeatmapLimitMessage(500, "PRO");
        
        assertTrue(message.contains("monthly limit of 500 seat map requests"));
        assertTrue(message.contains("PRO tier"));
        assertTrue(message.contains("Upgrade for more access"));
    }
    
    @Test
    void updateTimestamp_ShouldUpdateUpdatedAt() {
        Instant originalTime = userUsageHistory.getUpdatedAt();
        
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        userUsageHistory.updateTimestamp();
        
        assertTrue(userUsageHistory.getUpdatedAt().isAfter(originalTime));
    }
    
    @Test
    void multipleOperations_ShouldMaintainCorrectCounts() {
        // Record multiple bookmark creations
        userUsageHistory.recordBookmarkCreation();
        userUsageHistory.recordBookmarkCreation();
        userUsageHistory.recordBookmarkCreation();
        
        // Record multiple seatmap requests
        userUsageHistory.recordSeatmapRequest();
        userUsageHistory.recordSeatmapRequest();
        
        assertEquals(Integer.valueOf(3), userUsageHistory.getBookmarksCreated());
        assertEquals(Integer.valueOf(2), userUsageHistory.getSeatmapRequestsUsed());
        assertNotNull(userUsageHistory.getLastBookmarkCreated());
        assertNotNull(userUsageHistory.getLastSeatmapRequest());
    }
}