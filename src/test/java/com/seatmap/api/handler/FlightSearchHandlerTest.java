package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.auth.service.JwtService;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.auth.repository.BookmarkRepository;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.api.service.SabreService;
import com.seatmap.common.model.Bookmark;
import com.fasterxml.jackson.databind.JsonNode;
import com.seatmap.api.model.SeatMapData;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightSearchHandlerTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private JwtService mockJwtService;
    
    @Mock
    private Claims mockClaims;
    
    @Mock
    private BookmarkRepository mockBookmarkRepository;
    
    @Mock
    private AmadeusService mockAmadeusService;
    
    @Mock
    private SabreService mockSabreService;

    private FlightSearchHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        handler = new FlightSearchHandler();
        objectMapper = new ObjectMapper();
        
        // Inject mock services using reflection
        injectMock("jwtService", mockJwtService);
        injectMock("bookmarkRepository", mockBookmarkRepository);
        injectMock("amadeusService", mockAmadeusService);
        injectMock("sabreService", mockSabreService);
    }
    
    private void injectMock(String fieldName, Object mock) throws Exception {
        Field field = FlightSearchHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, mock);
    }

    @Test
    void testMissingAuthorizationHeader() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        event.setHeaders(new HashMap<>());
        event.setBody("{\"origin\":\"LAX\",\"destination\":\"JFK\",\"departureDate\":\"2024-12-15\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing or invalid authorization header"));
    }

    @Test
    void testInvalidAuthorizationHeader() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Invalid token");
        event.setHeaders(headers);
        event.setBody("{\"origin\":\"LAX\",\"destination\":\"JFK\",\"departureDate\":\"2024-12-15\"}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing or invalid authorization header"));
    }

    @Test
    void testInvalidRequestBody() throws SeatmapException {
        // Given - Mock JWT to pass validation so we can test JSON parsing
        when(mockJwtService.getUserIdFromToken("test-token")).thenReturn("test-user-id");
        when(mockJwtService.isGuestToken("test-token")).thenReturn(true); // Guest token bypasses validateToken
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        event.setBody("invalid json");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid request format"));
    }

    @Test
    void testBookmarkRequestWithoutBookmarkId() throws SeatmapException {
        // Given - Mock JWT methods for guest token flow
        when(mockJwtService.getUserIdFromToken("test-token")).thenReturn("test-user-id");
        when(mockJwtService.isGuestToken("test-token")).thenReturn(true);
        
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/");
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        event.setBody(""); // GET request typically has no body

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        // Should not match bookmark pattern (needs ID) and fall through to regular flow
        // JWT validation passes as guest, then JSON parsing fails
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void testCorsHeaders() {
        // Given
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");
        event.setPath("/flight-search");
        event.setHeaders(new HashMap<>());
        event.setBody("{}");

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertNotNull(response.getHeaders());
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
        assertEquals("GET,POST,PUT,DELETE,OPTIONS", response.getHeaders().get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type,Authorization,X-API-Key", response.getHeaders().get("Access-Control-Allow-Headers"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void testValidBookmarkPath() throws SeatmapException {
        // Given - Mock token validation to fail for bookmark access
        doThrow(new SeatmapException("TOKEN_INVALID", "Invalid token", 401))
            .when(mockJwtService).validateToken("test-token");
        
        String bookmarkId = "test-bookmark-123";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/" + bookmarkId);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Then
        assertEquals(401, response.getStatusCode()); // Token validation failed
        assertTrue(response.getBody().contains("Invalid or expired token"));
    }
    
    @Test
    void testBookmarkFlowWithAmadeusSeatMapConversion() throws Exception {
        // Given - Setup valid bookmark with Amadeus data
        String userId = "test-user-123";
        String bookmarkId = "test-bookmark-456";
        String flightOfferJson = """
        {
            "id": "test-offer-123",
            "type": "flight-offer",
            "dataSource": "AMADEUS",
            "itineraries": [{
                "segments": [{
                    "carrierCode": "UA",
                    "number": "1679",
                    "departure": {
                        "iataCode": "SFO",
                        "at": "2025-12-05T08:30:00"
                    },
                    "arrival": {
                        "iataCode": "CUN",
                        "at": "2025-12-05T16:45:00"
                    }
                }]
            }]
        }
        """;
        
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setBookmarkId(bookmarkId);
        bookmark.setFlightOfferData(flightOfferJson);
        bookmark.setCreatedAt(Instant.now());
        bookmark.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60));
        
        // Mock token validation
        when(mockJwtService.validateToken("test-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("test-token")).thenReturn(false);
        when(mockClaims.getSubject()).thenReturn(userId);
        
        // Mock bookmark retrieval
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId))
            .thenReturn(Optional.of(bookmark));
        
        // Mock Amadeus seat map response
        String amadeusSeatMapResponse = """
        {
            "data": [{
                "type": "seat-map",
                "number": "1679",
                "carrierCode": "UA",
                "aircraft": {"code": "73H"},
                "departure": {
                    "iataCode": "SFO",
                    "at": "2025-12-05T08:30:00"
                },
                "arrival": {
                    "iataCode": "CUN",
                    "at": "2025-12-05T16:45:00"
                },
                "decks": [{
                    "deckType": "MAIN",
                    "seats": [{
                        "number": "12A",
                        "characteristicsCodes": ["W"],
                        "travelerPricing": [{
                            "travelerId": "1",
                            "seatAvailabilityStatus": "AVAILABLE",
                            "price": {
                                "currency": "USD",
                                "total": "25.00"
                            }
                        }]
                    }]
                }]
            }]
        }
        """;
        
        JsonNode seatMapNode = objectMapper.readTree(amadeusSeatMapResponse);
        when(mockAmadeusService.getSeatMapFromOffer(any(JsonNode.class)))
            .thenReturn(seatMapNode);
        
        // Mock seat map conversion
        SeatMapData convertedSeatMap = new SeatMapData();
        convertedSeatMap.setSource("AMADEUS");
        SeatMapData.FlightInfo flightInfo = new SeatMapData.FlightInfo();
        flightInfo.setNumber("1679");
        flightInfo.setCarrierCode("UA");
        convertedSeatMap.setFlight(flightInfo);
        when(mockAmadeusService.convertToSeatMapData(seatMapNode))
            .thenReturn(convertedSeatMap);
        
        // Setup request
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/" + bookmarkId);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        
        // Verify the response contains properly converted seat map data
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        JsonNode responseJson = objectMapper.readTree(responseBody);
        assertTrue(responseJson.has("seatMap"));
        assertEquals("AMADEUS", responseJson.get("seatMap").get("source").asText());
        assertEquals("1679", responseJson.get("seatMap").get("flight").get("number").asText());
        assertEquals("UA", responseJson.get("seatMap").get("flight").get("carrierCode").asText());
        
        // Verify service calls
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(userId, bookmarkId);
        verify(mockAmadeusService).getSeatMapFromOffer(any(JsonNode.class));
        verify(mockAmadeusService).convertToSeatMapData(seatMapNode);
        verify(mockSabreService, never()).getSeatMapFromFlight(any(), any(), any(), any(), any());
    }
    
    @Test
    void testBookmarkFlowWithSabreSeatMapConversion() throws Exception {
        // Given - Setup valid bookmark with Sabre data
        String userId = "test-user-123";
        String bookmarkId = "test-bookmark-789";
        String flightOfferJson = """
        {
            "id": "test-offer-456",
            "type": "flight-offer",
            "dataSource": "SABRE",
            "itineraries": [{
                "segments": [{
                    "carrierCode": "AA",
                    "number": "1234",
                    "departure": {
                        "iataCode": "LAX",
                        "at": "2025-12-06T10:00:00"
                    },
                    "arrival": {
                        "iataCode": "JFK",
                        "at": "2025-12-06T18:30:00"
                    }
                }]
            }]
        }
        """;
        
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setBookmarkId(bookmarkId);
        bookmark.setFlightOfferData(flightOfferJson);
        bookmark.setCreatedAt(Instant.now());
        bookmark.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60));
        
        // Mock token validation
        when(mockJwtService.validateToken("test-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("test-token")).thenReturn(false);
        when(mockClaims.getSubject()).thenReturn(userId);
        
        // Mock bookmark retrieval
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId))
            .thenReturn(Optional.of(bookmark));
        
        // Mock Sabre seat map response
        String sabreSeatMapResponse = """
        {
            "GetSeatMapRS": {
                "FlightInfo": {
                    "OriginLocation": {"LocationCode": "LAX"},
                    "DestinationLocation": {"LocationCode": "JFK"},
                    "MarketingAirline": {"Code": "AA"},
                    "FlightNumber": "1234"
                },
                "Aircraft": {"Code": "321"},
                "SeatMapDetails": [{
                    "CabinClass": {"Name": "Economy"},
                    "RowInfo": [{
                        "RowNumber": "15",
                        "SeatInfo": [{
                            "SeatNumber": "F",
                            "Summary": "Window Available"
                        }]
                    }]
                }]
            }
        }
        """;
        
        JsonNode sabreSeatMapNode = objectMapper.readTree(sabreSeatMapResponse);
        when(mockSabreService.getSeatMapFromFlight("AA", "1234", "2025-12-06", "LAX", "JFK"))
            .thenReturn(sabreSeatMapNode);
        
        // Mock seat map conversion
        SeatMapData convertedSeatMap = new SeatMapData();
        convertedSeatMap.setSource("SABRE");
        SeatMapData.FlightInfo flightInfo = new SeatMapData.FlightInfo();
        flightInfo.setNumber("1234");
        flightInfo.setCarrierCode("AA");
        convertedSeatMap.setFlight(flightInfo);
        when(mockSabreService.convertToSeatMapData(sabreSeatMapNode))
            .thenReturn(convertedSeatMap);
        
        // Setup request
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/" + bookmarkId);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Then
        assertEquals(200, response.getStatusCode());
        
        // Verify the response contains properly converted seat map data
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        JsonNode responseJson = objectMapper.readTree(responseBody);
        assertTrue(responseJson.has("seatMap"));
        assertEquals("SABRE", responseJson.get("seatMap").get("source").asText());
        assertEquals("1234", responseJson.get("seatMap").get("flight").get("number").asText());
        assertEquals("AA", responseJson.get("seatMap").get("flight").get("carrierCode").asText());
        
        // Verify service calls
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(userId, bookmarkId);
        verify(mockSabreService).getSeatMapFromFlight("AA", "1234", "2025-12-06", "LAX", "JFK");
        verify(mockSabreService).convertToSeatMapData(sabreSeatMapNode);
        verify(mockAmadeusService, never()).getSeatMapFromOffer(any(JsonNode.class));
    }
    
    @Test
    void testBookmarkFlowWithExpiredBookmark() throws Exception {
        // Given - Setup expired bookmark
        String userId = "test-user-123";
        String bookmarkId = "expired-bookmark";
        
        Bookmark expiredBookmark = new Bookmark();
        expiredBookmark.setUserId(userId);
        expiredBookmark.setBookmarkId(bookmarkId);
        expiredBookmark.setFlightOfferData("{}");
        expiredBookmark.setCreatedAt(Instant.now().minusSeconds(35 * 24 * 60 * 60));
        expiredBookmark.setExpiresAt(Instant.now().minusSeconds(5 * 24 * 60 * 60)); // Expired 5 days ago
        
        // Mock token validation
        when(mockJwtService.validateToken("test-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("test-token")).thenReturn(false);
        when(mockClaims.getSubject()).thenReturn(userId);
        
        // Mock bookmark retrieval
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId))
            .thenReturn(Optional.of(expiredBookmark));
        
        // Setup request
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/" + bookmarkId);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Then
        assertEquals(410, response.getStatusCode()); // Gone - expired
        assertTrue(response.getBody().contains("Bookmark has expired"));
        
        // Verify no seat map service calls were made
        verify(mockAmadeusService, never()).getSeatMapFromOffer(any(JsonNode.class));
        verify(mockSabreService, never()).getSeatMapFromFlight(any(), any(), any(), any(), any());
    }
    
    @Test
    void testBookmarkFlowWithGuestToken() throws Exception {
        // Given - Guest token (not allowed for bookmark access)
        String bookmarkId = "test-bookmark";
        
        when(mockJwtService.validateToken("guest-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("guest-token")).thenReturn(true);
        
        // Setup request
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/" + bookmarkId);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer guest-token");
        event.setHeaders(headers);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Then
        assertEquals(401, response.getStatusCode());
        assertTrue(response.getBody().contains("Valid user authentication required for bookmark access"));
        
        // Verify no repository or service calls were made
        verify(mockBookmarkRepository, never()).findByUserIdAndBookmarkId(any(), any());
        verify(mockAmadeusService, never()).getSeatMapFromOffer(any(JsonNode.class));
        verify(mockSabreService, never()).getSeatMapFromFlight(any(), any(), any(), any(), any());
    }
    
    @Test
    void testBookmarkFlowWithSeatMapError() throws Exception {
        // Given - Setup valid bookmark but seat map retrieval fails
        String userId = "test-user-123";
        String bookmarkId = "test-bookmark-error";
        String flightOfferJson = """
        {
            "id": "test-offer-error",
            "type": "flight-offer",
            "dataSource": "AMADEUS",
            "itineraries": [{
                "segments": [{
                    "carrierCode": "UA",
                    "number": "1679"
                }]
            }]
        }
        """;
        
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setBookmarkId(bookmarkId);
        bookmark.setFlightOfferData(flightOfferJson);
        bookmark.setCreatedAt(Instant.now());
        bookmark.setExpiresAt(Instant.now().plusSeconds(30 * 24 * 60 * 60));
        
        // Mock token validation
        when(mockJwtService.validateToken("test-token")).thenReturn(mockClaims);
        when(mockJwtService.isGuestToken("test-token")).thenReturn(false);
        when(mockClaims.getSubject()).thenReturn(userId);
        
        // Mock bookmark retrieval
        when(mockBookmarkRepository.findByUserIdAndBookmarkId(userId, bookmarkId))
            .thenReturn(Optional.of(bookmark));
        
        // Mock seat map service to throw exception
        when(mockAmadeusService.getSeatMapFromOffer(any(JsonNode.class)))
            .thenThrow(new RuntimeException("API temporarily unavailable"));
        
        // Setup request
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("GET");
        event.setPath("/flight-search/bookmark/" + bookmarkId);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer test-token");
        event.setHeaders(headers);
        
        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Then
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Error retrieving flight data"));
        
        // Verify service was called but failed
        verify(mockBookmarkRepository).findByUserIdAndBookmarkId(userId, bookmarkId);
        verify(mockAmadeusService).getSeatMapFromOffer(any(JsonNode.class));
    }
}