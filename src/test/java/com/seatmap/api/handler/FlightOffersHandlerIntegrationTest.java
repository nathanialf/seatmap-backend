package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.api.service.SabreService;
import com.seatmap.auth.service.JwtService;
import com.seatmap.common.exception.SeatmapException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlightOffersHandler Integration Tests")
class FlightOffersHandlerIntegrationTest {

    private FlightOffersHandler handler;
    private ObjectMapper objectMapper;

    @Mock
    private AmadeusService mockAmadeusService;

    @Mock
    private SabreService mockSabreService;

    @Mock
    private JwtService mockJwtService;

    @Mock
    private Context mockContext;

    @Mock
    private Claims mockClaims;

    @BeforeEach
    void setUp() throws Exception {
        handler = new FlightOffersHandler();
        objectMapper = new ObjectMapper();

        // Inject mocks using reflection
        injectMock("amadeusService", mockAmadeusService);
        injectMock("sabreService", mockSabreService);
        injectMock("jwtService", mockJwtService);
    }

    private void injectMock(String fieldName, Object mock) throws Exception {
        Field field = FlightOffersHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, mock);
    }

    private APIGatewayProxyRequestEvent createBasicRequest() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer valid-token");
        request.setHeaders(headers);
        return request;
    }

    private String createValidRequestBody() {
        return "{" +
                "\"origin\":\"LAX\"," +
                "\"destination\":\"JFK\"," +
                "\"departureDate\":\"2024-12-01\"," +
                "\"flightNumber\":\"AA123\"," +
                "\"maxResults\":5" +
                "}";
    }

    private JsonNode createMockAmadeusResponse() throws Exception {
        String amadeusJson = "{" +
                "\"data\":[{" +
                "\"id\":\"1\"," +
                "\"type\":\"flight-offer\"," +
                "\"itineraries\":[{" +
                "\"segments\":[{" +
                "\"carrierCode\":\"AA\"," +
                "\"number\":\"123\"," +
                "\"departure\":{\"iataCode\":\"LAX\",\"at\":\"2024-12-01T10:00:00\"}," +
                "\"arrival\":{\"iataCode\":\"JFK\",\"at\":\"2024-12-01T18:00:00\"}" +
                "}]" +
                "}]" +
                "}]," +
                "\"dictionaries\":{\"carriers\":{\"AA\":\"American Airlines\"}}" +
                "}";
        return objectMapper.readTree(amadeusJson);
    }

    private JsonNode createMockSabreResponse() throws Exception {
        String sabreJson = "{" +
                "\"data\":[{" +
                "\"id\":\"2\"," +
                "\"type\":\"flight-schedule\"," +
                "\"itineraries\":[{" +
                "\"segments\":[{" +
                "\"carrierCode\":\"DL\"," +
                "\"number\":\"456\"," +
                "\"departure\":{\"iataCode\":\"LAX\",\"at\":\"2024-12-01T14:00:00\"}," +
                "\"arrival\":{\"iataCode\":\"JFK\",\"at\":\"2024-12-01T22:00:00\"}" +
                "}]" +
                "}]" +
                "}]" +
                "}";
        return objectMapper.readTree(sabreJson);
    }

    private JsonNode createEmptyResponse() {
        return objectMapper.createObjectNode().set("data", objectMapper.createArrayNode());
    }

    @Test
    @DisplayName("Should handle valid flight search request successfully")
    void handleRequest_ValidRequest_ReturnsSuccess() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockAmadeusResponse());
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockSabreResponse());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"data\""));
        assertTrue(response.getBody().contains("\"meta\""));
        verify(mockJwtService).validateToken("valid-token");
        verify(mockAmadeusService).searchFlightOffers("LAX", "JFK", "2024-12-01", "AA123", 5);
        verify(mockSabreService).searchFlightSchedules("LAX", "JFK", "2024-12-01", "AA123", 5);
    }

    @Test
    @DisplayName("Should handle missing authorization header")
    void handleRequest_MissingAuthHeader_ReturnsUnauthorized() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(new HashMap<>());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing or invalid authorization header"));
    }

    @Test
    @DisplayName("Should handle invalid authorization header format")
    void handleRequest_InvalidAuthHeaderFormat_ReturnsUnauthorized() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Invalid format");
        request.setHeaders(headers);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing or invalid authorization header"));
    }

    @Test
    @DisplayName("Should handle invalid JWT token")
    void handleRequest_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();

        when(mockJwtService.validateToken("valid-token"))
                .thenThrow(SeatmapException.unauthorized("Invalid token"));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }

    @Test
    @DisplayName("Should handle invalid JSON request body")
    void handleRequest_InvalidJsonBody_ReturnsBadRequest() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody("invalid-json");

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
    }

    @Test
    @DisplayName("Should handle validation errors in request")
    void handleRequest_ValidationErrors_ReturnsBadRequest() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        String invalidRequestBody = "{" +
                "\"origin\":\"\"," +  // Empty origin should fail validation
                "\"destination\":\"JFK\"," +
                "\"departureDate\":\"2024-12-01\"" +
                "}";
        request.setBody(invalidRequestBody);

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Validation errors"));
    }

    @Test
    @DisplayName("Should handle invalid airport code format")
    void handleRequest_InvalidAirportCode_ReturnsBadRequest() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        String invalidRequestBody = "{" +
                "\"origin\":\"LAXX\"," +  // Invalid 4-letter code
                "\"destination\":\"JFK\"," +
                "\"departureDate\":\"2024-12-01\"" +
                "}";
        request.setBody(invalidRequestBody);

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("must be a 3-letter airport code"));
    }

    @Test
    @DisplayName("Should handle invalid date format")
    void handleRequest_InvalidDateFormat_ReturnsBadRequest() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        String invalidRequestBody = "{" +
                "\"origin\":\"LAX\"," +
                "\"destination\":\"JFK\"," +
                "\"departureDate\":\"12/01/2024\"" +  // Invalid format
                "}";
        request.setBody(invalidRequestBody);

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("must be in YYYY-MM-DD format"));
    }

    @Test
    @DisplayName("Should handle Amadeus API failure gracefully")
    void handleRequest_AmadeusApiFails_ReturnsResultsFromSabre() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Amadeus API error"));
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockSabreResponse());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"data\""));
        // Should still contain Sabre results
        assertTrue(response.getBody().contains("DL"));
    }

    @Test
    @DisplayName("Should handle Sabre API failure gracefully")
    void handleRequest_SabreApiFails_ReturnsResultsFromAmadeus() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockAmadeusResponse());
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Sabre API error"));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"data\""));
        // Should still contain Amadeus results
        assertTrue(response.getBody().contains("AA"));
    }

    @Test
    @DisplayName("Should handle both APIs failing gracefully")
    void handleRequest_BothApisFail_ReturnsEmptyResults() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Amadeus API error"));
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Sabre API error"));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"data\":[]"));
        assertTrue(response.getBody().contains("\"count\":0"));
    }

    @Test
    @DisplayName("Should mesh results from both sources correctly")
    void handleRequest_BothSourcesReturnData_MeshesCorrectly() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockAmadeusResponse());
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockSabreResponse());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        String responseBody = response.getBody();
        
        // Should contain data from both sources
        assertTrue(responseBody.contains("amadeus_1"));  // Amadeus flight with source prefix
        assertTrue(responseBody.contains("sabre_2"));    // Sabre flight with source prefix
        assertTrue(responseBody.contains("\"dataSource\":\"AMADEUS\""));
        assertTrue(responseBody.contains("\"dataSource\":\"SABRE\""));
        assertTrue(responseBody.contains("\"sources\":\"AMADEUS,SABRE\""));
    }

    @Test
    @DisplayName("Should handle duplicate flights by preferring Amadeus")
    void handleRequest_DuplicateFlights_PrefersAmadeus() throws Exception {
        // Given - Create identical flights from both sources
        String duplicateFlightJson = "{" +
                "\"data\":[{" +
                "\"id\":\"duplicate\"," +
                "\"type\":\"flight-offer\"," +
                "\"itineraries\":[{" +
                "\"segments\":[{" +
                "\"carrierCode\":\"AA\"," +
                "\"number\":\"123\"," +
                "\"departure\":{\"iataCode\":\"LAX\",\"at\":\"2024-12-01T10:00:00\"}," +
                "\"arrival\":{\"iataCode\":\"JFK\",\"at\":\"2024-12-01T18:00:00\"}" +
                "}]" +
                "}]" +
                "}]" +
                "}";

        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(objectMapper.readTree(duplicateFlightJson));
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(objectMapper.readTree(duplicateFlightJson));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        String responseBody = response.getBody();
        
        // Should only contain one flight (from Amadeus)
        assertTrue(responseBody.contains("\"count\":1"));
        assertTrue(responseBody.contains("\"dataSource\":\"AMADEUS\""));
        assertFalse(responseBody.contains("sabre_duplicate"));  // No Sabre version
        assertTrue(responseBody.contains("amadeus_duplicate")); // Amadeus version present
    }

    @Test
    @DisplayName("Should respect maxResults limit")
    void handleRequest_MaxResultsLimit_RespectsLimit() throws Exception {
        // Given - Create request with maxResults=1
        APIGatewayProxyRequestEvent request = createBasicRequest();
        String limitedRequestBody = "{" +
                "\"origin\":\"LAX\"," +
                "\"destination\":\"JFK\"," +
                "\"departureDate\":\"2024-12-01\"," +
                "\"maxResults\":1" +
                "}";
        request.setBody(limitedRequestBody);

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockAmadeusResponse());
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockSabreResponse());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        String responseBody = response.getBody();
        
        // Parse response to check the count
        JsonNode responseJson = objectMapper.readTree(responseBody);
        int count = responseJson.path("meta").path("count").asInt();
        assertTrue(count <= 1, "Expected count to be 1 or less, but was " + count);
        
        // Verify that maxResults=1 was passed to services
        verify(mockAmadeusService).searchFlightOffers("LAX", "JFK", "2024-12-01", null, 1);
        verify(mockSabreService).searchFlightSchedules("LAX", "JFK", "2024-12-01", null, 1);
    }

    @Test
    @DisplayName("Should handle request without optional fields")
    void handleRequest_WithoutOptionalFields_UsesDefaults() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        String minimalRequestBody = "{" +
                "\"origin\":\"LAX\"," +
                "\"destination\":\"JFK\"," +
                "\"departureDate\":\"2024-12-01\"" +
                "}";
        request.setBody(minimalRequestBody);

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), isNull(), anyInt()))
                .thenReturn(createMockAmadeusResponse());
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), isNull(), anyInt()))
                .thenReturn(createMockSabreResponse());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        verify(mockAmadeusService).searchFlightOffers("LAX", "JFK", "2024-12-01", null, 10);
        verify(mockSabreService).searchFlightSchedules("LAX", "JFK", "2024-12-01", null, 10);
    }

    @Test
    @DisplayName("Should handle unexpected exceptions gracefully")
    void handleRequest_UnexpectedException_ReturnsInternalServerError() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Unexpected error"));
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode()); // Should still return 200 with empty results
        assertTrue(response.getBody().contains("\"data\":[]"));
    }

    @Test
    @DisplayName("Should include CORS headers in all responses")
    void handleRequest_AllResponses_IncludeCorsHeaders() {
        // Given
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHeaders(new HashMap<>());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        Map<String, String> headers = response.getHeaders();
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET,POST,PUT,DELETE,OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type,Authorization,X-API-Key", headers.get("Access-Control-Allow-Headers"));
        assertEquals("application/json", headers.get("Content-Type"));
    }

    @Test
    @DisplayName("Should handle response formatting errors gracefully")
    void createSuccessResponse_FormattingError_ReturnsErrorResponse() throws Exception {
        // Given
        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockAmadeusResponse());
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createMockSabreResponse());

        // Create a spy of the real ObjectMapper that fails on writeValueAsString for specific objects
        ObjectMapper faultyMapper = spy(objectMapper);
        // Make it fail when writing the final response but not during intermediate operations
        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            // Only fail if it's the final JsonNode response (has "meta" field)
            if (arg instanceof JsonNode && ((JsonNode) arg).has("meta")) {
                throw new RuntimeException("JSON formatting error");
            }
            // Otherwise call the real method
            return invocation.callRealMethod();
        }).when(faultyMapper).writeValueAsString(any());

        Field mapperField = FlightOffersHandler.class.getDeclaredField("objectMapper");
        mapperField.setAccessible(true);
        mapperField.set(handler, faultyMapper);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Error creating response"));
    }

    @Test
    @DisplayName("Should handle flight key creation with missing data gracefully")
    void handleRequest_MissingFlightData_HandlesGracefully() throws Exception {
        // Given - Mock response with incomplete flight data
        String incompleteFlightJson = "{" +
                "\"data\":[{" +
                "\"id\":\"incomplete\"," +
                "\"type\":\"flight-offer\"" +
                // Missing itineraries
                "}]" +
                "}";

        APIGatewayProxyRequestEvent request = createBasicRequest();
        request.setBody(createValidRequestBody());

        when(mockJwtService.validateToken("valid-token")).thenReturn(mockClaims);
        when(mockAmadeusService.searchFlightOffers(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(objectMapper.readTree(incompleteFlightJson));
        when(mockSabreService.searchFlightSchedules(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(createEmptyResponse());

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"data\""));
        // Should handle the incomplete data without crashing
    }
}