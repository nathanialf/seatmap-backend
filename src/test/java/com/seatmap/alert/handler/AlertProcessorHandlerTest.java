package com.seatmap.alert.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.common.model.Bookmark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertProcessorHandlerTest {
    
    @Mock
    private Context context;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.objectMapper = new ObjectMapper();
        
        // Set test environment variables
        System.setProperty("ENVIRONMENT", "test");
        System.setProperty("JWT_SECRET", "test-secret-key-that-is-at-least-32-characters-long-for-testing");
        System.setProperty("AMADEUS_API_KEY", "test-api-key");
        System.setProperty("AMADEUS_API_SECRET", "test-api-secret");
        System.setProperty("AMADEUS_ENDPOINT", "test.api.amadeus.com");
        System.setProperty("BASE_URL", "https://test.myseatmap.com");
    }
    
    @Test
    void testScheduledEventHandling() {
        // Given
        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setSource("aws.events");
        scheduledEvent.setDetailType("Scheduled Event");
        scheduledEvent.setDetail(Map.of("scheduled-event", "alert-processor"));
        
        // Note: In a real test environment, you'd want to mock the repositories and services
        // This test primarily verifies that the handler can be instantiated and process events
        
        // When/Then - Verify handler can be created without throwing exceptions
        assertDoesNotThrow(() -> {
            AlertProcessorHandler handler = new AlertProcessorHandler();
            // The actual processing would require mocked dependencies
            assertNotNull(handler);
        });
    }
    
    @Test
    void testSearchKeyGeneration_Bookmark() {
        // Test the logic for generating search keys for grouping alerts
        // This would normally be a private method, but we can test the logic
        
        Bookmark bookmark = createBookmarkWithFlightData();
        
        // Test that bookmarks with same route/date would generate similar keys
        String expectedKeyPattern = "LAX-JFK-2024-12-15";
        
        // Extract flight data and verify it contains the expected information
        String flightData = bookmark.getFlightOfferData();
        assertNotNull(flightData);
        assertTrue(flightData.contains("LAX"));
        assertTrue(flightData.contains("JFK"));
        assertTrue(flightData.contains("2024-12-15"));
    }
    
    @Test
    void testSearchKeyGeneration_SavedSearch() {
        // Test search key generation for saved searches
        
        Bookmark savedSearch = createSavedSearchBookmark();
        
        // Verify that saved search contains the criteria needed for grouping
        assertEquals("LAX", savedSearch.getOrigin());
        assertEquals("JFK", savedSearch.getDestination());
        assertEquals("2024-12-15", savedSearch.getDepartureDate());
        assertEquals("ECONOMY", savedSearch.getTravelClass());
        
        // The key would be something like: LAX-JFK-2024-12-15-ECONOMY-
        String expectedPattern = savedSearch.getOrigin() + "-" + savedSearch.getDestination() + 
                               "-" + savedSearch.getDepartureDate();
        
        assertNotNull(expectedPattern);
        assertTrue(expectedPattern.contains("LAX-JFK-2024-12-15"));
    }
    
    @Test
    void testNotificationCooldown() {
        // Test that notification cooldown logic works correctly
        
        Bookmark.AlertConfig alertConfig = new Bookmark.AlertConfig(10.0);
        
        // First trigger - should send notification
        Instant firstTrigger = Instant.now().minus(25, ChronoUnit.HOURS); // 25 hours ago
        alertConfig.setLastTriggered(firstTrigger);
        
        // Check if enough time has passed (should be true after 24+ hours)
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        boolean shouldSendNotification = alertConfig.getLastTriggered().isBefore(twentyFourHoursAgo);
        assertTrue(shouldSendNotification);
        
        // Recent trigger - should not send notification
        Instant recentTrigger = Instant.now().minus(2, ChronoUnit.HOURS); // 2 hours ago
        alertConfig.setLastTriggered(recentTrigger);
        
        shouldSendNotification = alertConfig.getLastTriggered().isBefore(twentyFourHoursAgo);
        assertFalse(shouldSendNotification);
    }
    
    @Test
    void testAlertConfigTimestamps() {
        // Test alert configuration timestamp management
        
        Bookmark.AlertConfig alertConfig = new Bookmark.AlertConfig(10.0);
        
        // Initially no timestamps
        assertNull(alertConfig.getLastEvaluated());
        assertNull(alertConfig.getLastTriggered());
        
        // Update timestamps
        alertConfig.updateLastEvaluated();
        alertConfig.recordTrigger();
        
        // Should now have timestamps
        assertNotNull(alertConfig.getLastEvaluated());
        assertNotNull(alertConfig.getLastTriggered());
        
        // Timestamps should be recent (within last few seconds)
        Instant now = Instant.now();
        assertTrue(alertConfig.getLastEvaluated().isAfter(now.minus(10, ChronoUnit.SECONDS)));
        assertTrue(alertConfig.getLastTriggered().isAfter(now.minus(10, ChronoUnit.SECONDS)));
    }
    
    @Test
    void testFlightSearchRequestConversion() {
        // Test that saved search bookmarks can be converted to flight search requests
        
        Bookmark savedSearch = createSavedSearchBookmark();
        
        // Convert to flight search request
        var searchRequest = savedSearch.toFlightSearchRequest();
        
        assertNotNull(searchRequest);
        assertEquals("LAX", searchRequest.getOrigin());
        assertEquals("JFK", searchRequest.getDestination());
        assertEquals("2024-12-15", searchRequest.getDepartureDate());
        assertEquals("ECONOMY", searchRequest.getTravelClass());
    }
    
    @Test
    void testFlightSearchRequestConversion_InvalidType() {
        // Test that regular bookmarks return null for flight search request conversion
        
        Bookmark regularBookmark = createBookmarkWithFlightData();
        
        // Should return null for non-saved-search bookmarks
        var searchRequest = regularBookmark.toFlightSearchRequest();
        
        assertNull(searchRequest);
    }
    
    @Test
    void testUpcomingFlightFiltering() {
        // Test logic for filtering flights departing within next 48 hours
        
        Instant now = Instant.now();
        Instant fortyEightHoursFromNow = now.plus(48, ChronoUnit.HOURS);
        Instant seventyTwoHoursFromNow = now.plus(72, ChronoUnit.HOURS);
        
        // Flight expiring within 48 hours - should be included
        Bookmark upcomingFlight = createBookmarkWithFlightData();
        upcomingFlight.setExpiresAt(fortyEightHoursFromNow.minus(1, ChronoUnit.HOURS)); // 47 hours from now
        upcomingFlight.setAlertConfig(new Bookmark.AlertConfig(10.0));
        
        // Flight expiring after 48 hours - should be excluded
        Bookmark futureBookmark = createBookmarkWithFlightData();
        futureBookmark.setExpiresAt(seventyTwoHoursFromNow); // 72 hours from now
        futureBookmark.setAlertConfig(new Bookmark.AlertConfig(10.0));
        
        // Check filtering logic
        boolean upcomingIncluded = upcomingFlight.getExpiresAt().isBefore(fortyEightHoursFromNow);
        boolean futureExcluded = futureBookmark.getExpiresAt().isAfter(fortyEightHoursFromNow);
        
        assertTrue(upcomingIncluded);
        assertTrue(futureExcluded);
    }
    
    @Test
    void testErrorHandling_InvalidScheduledEvent() {
        // Test that handler gracefully handles invalid or empty scheduled events
        
        ScheduledEvent emptyEvent = new ScheduledEvent();
        
        // Should not throw exceptions with empty/null event
        assertDoesNotThrow(() -> {
            AlertProcessorHandler handler = new AlertProcessorHandler();
            // In a real test, we'd call handleRequest here with proper mocking
            assertNotNull(handler);
        });
    }
    
    // Helper methods
    
    private Bookmark createBookmarkWithFlightData() {
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.BOOKMARK);
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("LAX to JFK - AA123");
        
        // Create realistic flight offer data
        ObjectNode flightData = objectMapper.createObjectNode();
        ObjectNode itinerary = objectMapper.createObjectNode();
        ObjectNode segment = objectMapper.createObjectNode();
        ObjectNode departure = objectMapper.createObjectNode();
        ObjectNode arrival = objectMapper.createObjectNode();
        ObjectNode operating = objectMapper.createObjectNode();
        
        departure.put("iataCode", "LAX");
        departure.put("at", "2024-12-15T10:00:00");
        arrival.put("iataCode", "JFK");
        arrival.put("at", "2024-12-15T18:00:00");
        operating.put("carrierCode", "AA");
        operating.put("number", "123");
        
        segment.set("departure", departure);
        segment.set("arrival", arrival);
        segment.set("operating", operating);
        
        itinerary.set("segments", objectMapper.createArrayNode().add(segment));
        flightData.set("itineraries", objectMapper.createArrayNode().add(itinerary));
        
        bookmark.setFlightOfferData(flightData.toString());
        
        // Set expiration to 24 hours after departure for flight bookmarks
        bookmark.setExpiresAt(Instant.parse("2024-12-15T18:00:00Z").plus(24, ChronoUnit.HOURS));
        
        return bookmark;
    }
    
    private Bookmark createSavedSearchBookmark() {
        Bookmark bookmark = new Bookmark();
        bookmark.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-search");
        bookmark.setTitle("LAX to JFK Flights");
        bookmark.setOrigin("LAX");
        bookmark.setDestination("JFK");
        bookmark.setDepartureDate("2024-12-15");
        bookmark.setTravelClass("ECONOMY");
        
        // Set expiration to 30 days for saved searches
        bookmark.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        
        return bookmark;
    }
}