package com.seatmap.common.model;

import com.seatmap.api.model.FlightSearchRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BookmarkFieldStructureTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void savedSearchBookmark_WithValidFields_PassesValidation() {
        // Arrange
        FlightSearchRequest searchRequest = new FlightSearchRequest("LAX", "JFK", "2025-12-15", "ECONOMY");
        searchRequest.setAirlineCode("AA");
        searchRequest.setFlightNumber("123");
        searchRequest.setMaxResults(10);

        // Act
        Bookmark bookmark = new Bookmark("user123", "bookmark456", "Test Search", searchRequest, Bookmark.ItemType.SAVED_SEARCH);

        // Assert
        assertEquals("user123", bookmark.getUserId());
        assertEquals("bookmark456", bookmark.getBookmarkId());
        assertEquals("Test Search", bookmark.getTitle());
        assertEquals(Bookmark.ItemType.SAVED_SEARCH, bookmark.getItemType());
        
        // Check individual fields are populated
        assertEquals("LAX", bookmark.getOrigin());
        assertEquals("JFK", bookmark.getDestination());
        assertEquals("2025-12-15", bookmark.getDepartureDate());
        assertEquals("ECONOMY", bookmark.getTravelClass());
        assertEquals("AA", bookmark.getAirlineCode());
        assertEquals("123", bookmark.getFlightNumber());
        assertEquals(10, bookmark.getMaxResults());
        
        // Should have expiration set
        assertNotNull(bookmark.getExpiresAt());
        assertTrue(bookmark.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void savedSearchBookmark_WithOptionalFieldsNull_PassesValidation() {
        // Arrange
        FlightSearchRequest searchRequest = new FlightSearchRequest("LAX", "JFK", "2025-12-15", null);
        // No airline code, flight number, or max results set

        // Act
        Bookmark bookmark = new Bookmark("user123", "bookmark456", "Test Search", searchRequest, Bookmark.ItemType.SAVED_SEARCH);

        // Assert
        assertEquals("LAX", bookmark.getOrigin());
        assertEquals("JFK", bookmark.getDestination());
        assertEquals("2025-12-15", bookmark.getDepartureDate());
        assertNull(bookmark.getTravelClass());
        assertNull(bookmark.getAirlineCode());
        assertNull(bookmark.getFlightNumber());
        assertEquals(10, bookmark.getMaxResults()); // Default value from FlightSearchRequest
    }

    @Test
    void savedSearchBookmark_ToFlightSearchRequest_ReconstructsCorrectly() {
        // Arrange
        FlightSearchRequest originalRequest = new FlightSearchRequest("SFO", "LAX", "2025-12-01", "BUSINESS");
        originalRequest.setAirlineCode("UA");
        originalRequest.setFlightNumber("456");
        originalRequest.setMaxResults(5);

        Bookmark bookmark = new Bookmark("user123", "bookmark456", "Test Search", originalRequest, Bookmark.ItemType.SAVED_SEARCH);

        // Act
        FlightSearchRequest reconstructedRequest = bookmark.toFlightSearchRequest();

        // Assert
        assertNotNull(reconstructedRequest);
        assertEquals("SFO", reconstructedRequest.getOrigin());
        assertEquals("LAX", reconstructedRequest.getDestination());
        assertEquals("2025-12-01", reconstructedRequest.getDepartureDate());
        assertEquals("BUSINESS", reconstructedRequest.getTravelClass());
        assertEquals("UA", reconstructedRequest.getAirlineCode());
        assertEquals("456", reconstructedRequest.getFlightNumber());
        assertEquals(5, reconstructedRequest.getMaxResults());
    }

    @Test
    void flightBookmark_ToFlightSearchRequest_ReturnsNull() {
        // Arrange
        Bookmark bookmark = new Bookmark("user123", "bookmark456", "Test Flight", "{\"flight\":\"data\"}", Bookmark.ItemType.BOOKMARK);

        // Act
        FlightSearchRequest request = bookmark.toFlightSearchRequest();

        // Assert
        assertNull(request);
    }

    @Test
    void savedSearchBookmark_ValidatesIndividualFields() {
        // Arrange
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("user123");
        bookmark.setBookmarkId("bookmark456");
        bookmark.setTitle("Test Search");
        bookmark.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        
        // Set invalid fields
        bookmark.setOrigin("INVALID"); // Too long
        bookmark.setDestination("XX"); // Too short
        bookmark.setDepartureDate("2025/01/15"); // Wrong format
        bookmark.setTravelClass("INVALID_CLASS");
        bookmark.setAirlineCode("INVALID"); // Too long
        bookmark.setFlightNumber("INVALID"); // Not numeric
        bookmark.setMaxResults(100); // Too high

        // Act
        Set<ConstraintViolation<Bookmark>> violations = validator.validate(bookmark);

        // Assert
        assertTrue(violations.size() >= 6); // Should have multiple validation errors
        
        // Check for specific validation messages
        boolean hasOriginError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Origin must be a 3-letter airport code"));
        boolean hasDestinationError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Destination must be a 3-letter airport code"));
        boolean hasDateError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Departure date must be in YYYY-MM-DD format"));
        boolean hasTravelClassError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Travel class must be ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST"));
        boolean hasAirlineCodeError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Airline code must be 2-3 uppercase letters"));
        boolean hasFlightNumberError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Flight number must be 1-4 digits"));
        boolean hasMaxResultsError = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Max results cannot exceed 50"));

        assertTrue(hasOriginError);
        assertTrue(hasDestinationError);
        assertTrue(hasDateError);
        assertTrue(hasTravelClassError);
        assertTrue(hasAirlineCodeError);
        assertTrue(hasFlightNumberError);
        assertTrue(hasMaxResultsError);
    }

    @Test
    void flightBookmark_DoesNotPopulateSavedSearchFields() {
        // Arrange & Act
        Bookmark bookmark = new Bookmark("user123", "bookmark456", "Test Flight", "{\"flight\":\"data\"}", Bookmark.ItemType.BOOKMARK);

        // Assert
        assertEquals(Bookmark.ItemType.BOOKMARK, bookmark.getItemType());
        assertEquals("{\"flight\":\"data\"}", bookmark.getFlightOfferData());
        
        // Saved search fields should be null
        assertNull(bookmark.getOrigin());
        assertNull(bookmark.getDestination());
        assertNull(bookmark.getDepartureDate());
        assertNull(bookmark.getTravelClass());
        assertNull(bookmark.getAirlineCode());
        assertNull(bookmark.getFlightNumber());
        assertNull(bookmark.getMaxResults());
    }
}