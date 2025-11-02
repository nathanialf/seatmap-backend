package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import jakarta.xml.soap.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SabreService Tests")
class SabreServiceTest {

    private SabreService sabreService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Environment variables are already set in build.gradle
        sabreService = new SabreService();
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should check session validity correctly")
        void shouldCheckSessionValidityCorrectly() {
            // Initially should be invalid (no token)
            assertFalse(sabreService.isSessionValid());
        }

        @Test
        @DisplayName("Should get session expiration time")
        void shouldGetSessionExpirationTime() {
            // Initially should be 0 (no session)
            assertEquals(0, sabreService.getSessionExpirationTime());
        }
    }

    @Nested
    @DisplayName("Flight Schedule Search Tests")
    class FlightScheduleSearchTests {

        @Test
        @DisplayName("Should validate required input parameters")
        void shouldValidateRequiredInputParameters() {
            // Test null origin
            assertThrows(SeatmapApiException.class, () ->
                sabreService.searchFlightSchedules(null, "JFK", "2024-12-01", "Economy", null, 10));

            // Test empty origin
            assertThrows(SeatmapApiException.class, () ->
                sabreService.searchFlightSchedules("", "JFK", "2024-12-01", "Economy", null, 10));

            // Test null destination
            assertThrows(SeatmapApiException.class, () ->
                sabreService.searchFlightSchedules("LAX", null, "2024-12-01", "Economy", null, 10));

            // Test null date
            assertThrows(SeatmapApiException.class, () ->
                sabreService.searchFlightSchedules("LAX", "JFK", null, "Economy", null, 10));
        }

        @Test
        @DisplayName("Should handle authentication attempts")
        void shouldHandleAuthenticationAttempts() {
            // Test that the service attempts authentication (will fail with test credentials)
            assertThrows(SeatmapApiException.class, () ->
                sabreService.searchFlightSchedules("LAX", "JFK", "2024-12-01", "Economy", null, 10));
        }
    }

    @Nested
    @DisplayName("Seat Map Tests")
    class SeatMapTests {

        @Test
        @DisplayName("Should validate seat map request parameters")
        void shouldValidateSeatMapRequestParameters() {
            // Test null carrier code
            assertThrows(SeatmapApiException.class, () ->
                sabreService.getSeatMapFromFlight(null, "123", "2024-12-01", "LAX", "JFK"));

            // Test empty carrier code
            assertThrows(SeatmapApiException.class, () ->
                sabreService.getSeatMapFromFlight("", "123", "2024-12-01", "LAX", "JFK"));

            // Test null flight number
            assertThrows(SeatmapApiException.class, () ->
                sabreService.getSeatMapFromFlight("AA", null, "2024-12-01", "LAX", "JFK"));

            // Test null date
            assertThrows(SeatmapApiException.class, () ->
                sabreService.getSeatMapFromFlight("AA", "123", null, "LAX", "JFK"));
        }

        @Test
        @DisplayName("Should handle authentication attempts for seat map")
        void shouldHandleAuthenticationAttemptsForSeatMap() {
            // Test that the service attempts authentication (will fail with test credentials)
            assertThrows(SeatmapApiException.class, () ->
                sabreService.getSeatMapFromFlight("AA", "123", "2024-12-01", "LAX", "JFK"));
        }
    }

    @Nested
    @DisplayName("Service Configuration Tests")
    class ServiceConfigurationTests {

        @Test
        @DisplayName("Should initialize service successfully")
        void shouldInitializeServiceSuccessfully() {
            assertNotNull(sabreService);
        }

        @Test
        @DisplayName("Should handle basic service operations")
        void shouldHandleBasicServiceOperations() {
            // Test basic operations don't throw unexpected exceptions
            assertDoesNotThrow(() -> sabreService.isSessionValid());
            assertDoesNotThrow(() -> sabreService.getSessionExpirationTime());
        }
    }
}