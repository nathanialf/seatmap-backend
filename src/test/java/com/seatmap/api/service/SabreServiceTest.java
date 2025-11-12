package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapApiException;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.api.model.SeatMapData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.xml.soap.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SabreService Tests")
class SabreServiceTest {

    @Mock
    private HttpClient mockHttpClient;
    
    @Mock
    private HttpResponse<String> mockHttpResponse;
    
    private SabreService sabreService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Environment variables are already set in build.gradle
        sabreService = new SabreService();
        
        // Inject mock HttpClient using reflection
        try {
            Field httpClientField = SabreService.class.getDeclaredField("httpClient");
            httpClientField.setAccessible(true);
            httpClientField.set(sabreService, mockHttpClient);
        } catch (Exception e) {
            // For tests without HTTP mocking
        }
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
    
    @Nested
    @DisplayName("Data Processing Tests")
    class DataProcessingTests {
        
        @Test
        @DisplayName("Should extract carrier code from flight JSON")
        void shouldExtractCarrierCodeFromFlightJson() throws Exception {
            // Test the private extractCarrierCode method using reflection
            Method extractCarrierCodeMethod = SabreService.class.getDeclaredMethod("extractCarrierCode", JsonNode.class);
            extractCarrierCodeMethod.setAccessible(true);
            
            // Create test flight JSON with correct Amadeus structure
            String flightJson = """
                {
                    "itineraries": [{
                        "segments": [{
                            "carrierCode": "AA"
                        }]
                    }]
                }
                """;
            JsonNode flight = objectMapper.readTree(flightJson);
            
            String result = (String) extractCarrierCodeMethod.invoke(sabreService, flight);
            assertEquals("AA", result);
        }
        
        @Test
        @DisplayName("Should extract flight number from flight JSON")
        void shouldExtractFlightNumberFromFlightJson() throws Exception {
            Method extractFlightNumberMethod = SabreService.class.getDeclaredMethod("extractFlightNumber", JsonNode.class);
            extractFlightNumberMethod.setAccessible(true);
            
            String flightJson = """
                {
                    "itineraries": [{
                        "segments": [{
                            "number": "1234"
                        }]
                    }]
                }
                """;
            JsonNode flight = objectMapper.readTree(flightJson);
            
            String result = (String) extractFlightNumberMethod.invoke(sabreService, flight);
            assertEquals("1234", result);
        }
        
        @Test
        @DisplayName("Should extract departure date from flight JSON")
        void shouldExtractDepartureDateFromFlightJson() throws Exception {
            Method extractDepartureDateMethod = SabreService.class.getDeclaredMethod("extractDepartureDate", JsonNode.class);
            extractDepartureDateMethod.setAccessible(true);
            
            String flightJson = """
                {
                    "itineraries": [{
                        "segments": [{
                            "departure": {
                                "at": "2024-12-01T08:30:00"
                            }
                        }]
                    }]
                }
                """;
            JsonNode flight = objectMapper.readTree(flightJson);
            
            String result = (String) extractDepartureDateMethod.invoke(sabreService, flight);
            assertEquals("2024-12-01", result);
        }
        
        @Test
        @DisplayName("Should extract origin from flight JSON")
        void shouldExtractOriginFromFlightJson() throws Exception {
            Method extractOriginMethod = SabreService.class.getDeclaredMethod("extractOrigin", JsonNode.class);
            extractOriginMethod.setAccessible(true);
            
            String flightJson = """
                {
                    "itineraries": [{
                        "segments": [{
                            "departure": {
                                "iataCode": "LAX"
                            }
                        }]
                    }]
                }
                """;
            JsonNode flight = objectMapper.readTree(flightJson);
            
            String result = (String) extractOriginMethod.invoke(sabreService, flight);
            assertEquals("LAX", result);
        }
        
        @Test
        @DisplayName("Should extract destination from flight JSON")
        void shouldExtractDestinationFromFlightJson() throws Exception {
            Method extractDestinationMethod = SabreService.class.getDeclaredMethod("extractDestination", JsonNode.class);
            extractDestinationMethod.setAccessible(true);
            
            String flightJson = """
                {
                    "itineraries": [{
                        "segments": [{
                            "arrival": {
                                "iataCode": "JFK"
                            }
                        }]
                    }]
                }
                """;
            JsonNode flight = objectMapper.readTree(flightJson);
            
            String result = (String) extractDestinationMethod.invoke(sabreService, flight);
            assertEquals("JFK", result);
        }
        
        @Test
        @DisplayName("Should build flight search result from JSON")
        void shouldBuildFlightSearchResultFromJson() throws Exception {
            // Create a spy to mock the getSeatMapFromFlight method
            SabreService spyService = Mockito.spy(sabreService);
            
            // Mock seat map response
            String mockSeatMapJson = """
                {
                    "flight": "AA1234",
                    "aircraft": "Boeing 737"
                }
                """;
            JsonNode mockSeatMapResponse = objectMapper.readTree(mockSeatMapJson);
            
            // Mock the getSeatMapFromFlight method to return our mock response
            Mockito.doReturn(mockSeatMapResponse)
                .when(spyService)
                .getSeatMapFromFlight(anyString(), anyString(), anyString(), anyString(), anyString());
            
            Method buildFlightSearchResultMethod = SabreService.class.getDeclaredMethod("buildFlightSearchResult", JsonNode.class);
            buildFlightSearchResultMethod.setAccessible(true);
            
            String flightJson = """
                {
                    "itineraries": [{
                        "segments": [{
                            "carrierCode": "AA",
                            "number": "1234",
                            "departure": {
                                "iataCode": "LAX",
                                "at": "2024-12-01T08:30:00"
                            },
                            "arrival": {
                                "iataCode": "JFK",
                                "at": "2024-12-01T16:45:00"
                            }
                        }]
                    }]
                }
                """;
            JsonNode flight = objectMapper.readTree(flightJson);
            
            FlightSearchResult result = (FlightSearchResult) buildFlightSearchResultMethod.invoke(spyService, flight);
            assertNotNull(result);
            assertEquals("SABRE", result.getDataSource());
        }
        
        @Test
        @DisplayName("Should convert seat map response to SeatMapData")
        void shouldConvertSeatMapResponseToSeatMapData() throws Exception {
            Method convertToSeatMapDataMethod = SabreService.class.getDeclaredMethod("convertToSeatMapData", JsonNode.class);
            convertToSeatMapDataMethod.setAccessible(true);
            
            String seatMapJson = """
                {
                    "flight": "AA1234",
                    "aircraft": "Boeing 737",
                    "seats": []
                }
                """;
            JsonNode seatMapResponse = objectMapper.readTree(seatMapJson);
            
            SeatMapData result = (SeatMapData) convertToSeatMapDataMethod.invoke(sabreService, seatMapResponse);
            assertNotNull(result);
            assertEquals("SABRE", result.getSource());
        }
        
        @Test
        @DisplayName("Should validate inputs correctly")
        void shouldValidateInputsCorrectly() throws Exception {
            Method validateInputsMethod = SabreService.class.getDeclaredMethod("validateInputs", String.class, String.class, String.class);
            validateInputsMethod.setAccessible(true);
            
            // Valid inputs should not throw
            assertDoesNotThrow(() -> validateInputsMethod.invoke(sabreService, "LAX", "JFK", "2024-12-01"));
            
            // Invalid inputs should throw
            assertThrows(Exception.class, () -> validateInputsMethod.invoke(sabreService, null, "JFK", "2024-12-01"));
            assertThrows(Exception.class, () -> validateInputsMethod.invoke(sabreService, "", "JFK", "2024-12-01"));
            assertThrows(Exception.class, () -> validateInputsMethod.invoke(sabreService, "LAX", null, "2024-12-01"));
            assertThrows(Exception.class, () -> validateInputsMethod.invoke(sabreService, "LAX", "", "2024-12-01"));
            assertThrows(Exception.class, () -> validateInputsMethod.invoke(sabreService, "LAX", "JFK", null));
            assertThrows(Exception.class, () -> validateInputsMethod.invoke(sabreService, "LAX", "JFK", ""));
        }
        
        @Test
        @DisplayName("Should parse environment variable with default")
        void shouldParseEnvironmentVariableWithDefault() throws Exception {
            Method parseIntEnvVarMethod = SabreService.class.getDeclaredMethod("parseIntEnvVar", String.class, int.class);
            parseIntEnvVarMethod.setAccessible(true);
            
            // Test with non-existent env var should return default
            int result = (Integer) parseIntEnvVarMethod.invoke(sabreService, "NON_EXISTENT_VAR", 42);
            assertEquals(42, result);
        }
        
        @Test
        @DisplayName("Should handle null and empty JSON nodes gracefully")
        void shouldHandleNullAndEmptyJsonNodesGracefully() throws Exception {
            Method extractCarrierCodeMethod = SabreService.class.getDeclaredMethod("extractCarrierCode", JsonNode.class);
            extractCarrierCodeMethod.setAccessible(true);
            
            // Test with empty JSON - should return default value "XX"
            JsonNode emptyNode = objectMapper.readTree("{}");
            String result = (String) extractCarrierCodeMethod.invoke(sabreService, emptyNode);
            assertEquals("XX", result);
            
            // Test missing fields too  
            JsonNode missingFields = objectMapper.readTree("{\"itineraries\": [{}]}");
            String result2 = (String) extractCarrierCodeMethod.invoke(sabreService, missingFields);
            assertEquals("XX", result2);
        }
    }
    
    // TODO: SeatMap Data Conversion Tests will be added when SabreService.convertToSeatMapData() is fully implemented
}